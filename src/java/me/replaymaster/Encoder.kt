package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.min

class Encoder(
        private val beatMap: BeatMap,
        private val baseImageFile: File,
        private val outFile: File,
        private val rate: Double,
        private val delay: Int,
        private val windowFrameCount: Int,
        private val ffmpegPath: String,
        private val gameWidth: Int
) {

    private val duration = beatMap.duration / 1000.0
    private val audioDelay = delay + beatMap.bgmOffset
    private val N = Config.INSTANCE.ffmpegMaxProcessingSize

    fun generateVideo() {
        if (beatMap.bgmPath == BeatMap.MUTE_MUSIC) {
            beatMap.bgmPath = baseImageFile.parentFile.resolve(beatMap.bgmPath).absolutePath
            debug("Generate mute sound: ${beatMap.bgmPath}")
            runFFMPEG(
                    arrayListOf("-f", "lavfi", "-t", "1", "-i", "anullsrc", beatMap.bgmPath, "-y"),
                    File(beatMap.bgmPath), 0.0, "", 0, 0)
        }

        var count = 0
        while (true) {
            if (File(getImageSliceName(count)).exists()) {
                count += 1
            } else {
                break
            }
        }

        if (count <= N) {
            debug("generate once!")
            generateVideo(0 until count, outFile, true, "merge", ReplayMaster.STAGE_RENDER, 100)
            return
        }

        val slices = arrayListOf<String>()
        val intervalProgress = (ReplayMaster.STAGE_SPLICT - ReplayMaster.STAGE_RENDER) / (count.toDouble())
        for (i in 0 until ceil(count / N.toDouble()).toInt()) {
            val startIndex = N * i
            val endIndex = min(startIndex + N, count) - 1
            val sliceVideo = baseImageFile.absolutePath.replace(".png", "_$i.mp4")
            debug("combine video #$startIndex -> #$endIndex to $sliceVideo")
            slices.add(sliceVideo)
            generateVideo(startIndex..endIndex, File(sliceVideo), false,
                    "split_$i",
                    ReplayMaster.STAGE_RENDER + (intervalProgress * startIndex).toInt(),
                    ReplayMaster.STAGE_RENDER + (intervalProgress * (endIndex + 1)).toInt())
        }
        mergeSlices(slices, duration, "merge", ReplayMaster.STAGE_SPLICT, 100)
    }

    private fun getImageSliceName(index: Int) = baseImageFile.absolutePath.replace(".png", "_$index.png")

    private fun mergeSlices(slices: List<String>, expectedDuration: Double, stage: String, startProgress: Int, endProgress: Int) {
        val filterGraph = arrayListOf(
                "[0:a]atempo=$rate[a1]",
                "[a1]adelay=$audioDelay|$audioDelay[audio]"
        )
        val command = arrayListOf(
                "-i", beatMap.bgmPath
        )
        val outVideos = arrayListOf<String>()
        for (i in slices.indices) {
            val path = slices[i]
            command.addAll(listOf("-i", path))
            outVideos.add("[${i + 1}:v]")
        }
        filterGraph.add("${outVideos.joinToString("")}concat=n=${slices.size}:v=1:a=0[video]")
        command.addAll(arrayListOf(
                "-filter_complex", filterGraph.joinToString(";"),
                "-map", "[video]",
                "-map", "[audio]",
                "-c:v", Config.INSTANCE.codec,
                "-preset", "ultrafast",
                "-benchmark",
                outFile.absolutePath
        ))
        runFFMPEG(command, outFile, expectedDuration, stage, startProgress, endProgress)
    }

    private fun generateVideo(inputIndicesRange: IntRange, out: File, isFinal: Boolean, stage: String, startProgress: Int, endProgress: Int) {
        out.delete()
        val filterGraph = arrayListOf<String>()
        val subcommands = arrayListOf<String>()
        val outVideos = arrayListOf<String>()
        val inputIndices = inputIndicesRange.toList()
        val sliceDuration = windowFrameCount.toDouble() / Config.INSTANCE.framePerSecond
        for (i in inputIndices.indices) {
            val fileIndex = inputIndices[i]
            val path = getImageSliceName(fileIndex)
            val inputFileIndex = if (isFinal) i + 1 else i
            val transpose = if (beatMap.isTaiko) {
                ",transpose=1"
            } else {
                ""
            }
            filterGraph.addAll(listOf(
                    "color=s=$gameWidth*${Config.INSTANCE.height},fps=${Config.INSTANCE.framePerSecond}[bg$i]",
                    "[bg$i][$inputFileIndex]overlay=y=-h+H+t*${Config.INSTANCE.framePerSecond * Config.INSTANCE.speed}:shortest=1[video${i}l]",
                    "[video${i}l]trim=end=$sliceDuration$transpose[video${i}]"
            ))
            outVideos.add("[video$i]")
            subcommands.addAll(listOf(
                    "-r", "1",
                    "-loop", "1",
                    "-i", path)
            )
        }
        filterGraph.add("${outVideos.joinToString("")}concat=n=${inputIndices.toList().size}:v=1[video]")
        val command = arrayListOf<String>()
        if (isFinal) {
            command.addAll(listOf("-i", beatMap.bgmPath))
            filterGraph.addAll(listOf(
                    "[0:a]atempo=$rate[a1]",
                    "[a1]adelay=$audioDelay|$audioDelay[audio]"
            ))
        }

        command.addAll(subcommands)
        command.addAll(arrayListOf(
                "-filter_complex", filterGraph.joinToString(";"),
                "-map", "[video]"))
        if (isFinal) {
            command.addAll(listOf("-map", "[audio]"))
        }
        command.addAll(arrayListOf(
                "-c:v", Config.INSTANCE.codec,
                "-preset", "ultrafast",
                "-benchmark",
                out.absolutePath
        ))
        runFFMPEG(command, out, sliceDuration * inputIndices.size, stage, startProgress, endProgress)
    }

    private fun runFFMPEG(command: MutableList<String>, out: File, expectedDuration: Double, stage: String, startProgress: Int, endProgress: Int) {
        command.add(0, ffmpegPath)

        if (Config.INSTANCE.isServer) {
            debug(command.joinToString(" "))
        }
        val startTime = System.currentTimeMillis()

        val ffmpeg = ProcessBuilder()
                .command(command)
                .redirectErrorStream(true)
                .start()
        val stdout = BufferedReader(InputStreamReader(ffmpeg.getInputStream()))
        var line: String
        fun parseTime(content: String): Float {
            try {
                val timePiece = content.split(":").map { it.toFloat() }
                return timePiece[0] * 60 * 60 + timePiece[1] * 60 + timePiece[2]
            } catch (exception: Exception) {
                return 0F
            }
        }

        val timeParser = Pattern.compile("time=(\\d+:\\d+:\\d+\\.\\d+)\\s")
        while (true) {
            line = stdout.readLine() ?: break
            debug("[FFMPEG] $line")
            if (expectedDuration != 0.0) {
                val matcher = timeParser.matcher(line)
                if (matcher.find()) {
                    val timeRead = parseTime(matcher.group(1))
                    val currentProgress = (timeRead.toDouble() / expectedDuration * 100)
                    ReplayMaster.printProgress(stage, startProgress, endProgress, currentProgress)
                }
            }
        }

        ffmpeg.waitFor()
        stdout.close()
        if (expectedDuration != 0.0) {
            ReplayMaster.printProgress(stage, startProgress, endProgress, 100.0)
        }
        debug("Render duration: ${(System.currentTimeMillis() - startTime) / 1000} s")

        if (!out.exists()) {
            throw VideoGenerationException(Exception(out.absolutePath))
        }
    }
}