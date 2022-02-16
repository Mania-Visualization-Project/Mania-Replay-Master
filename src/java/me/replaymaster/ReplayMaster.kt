package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


object ReplayMaster {

    val STAGE_RENDER = 30
    val STAGE_SPLICT = 60

    @JvmStatic
    fun generateVideo(beatMap: BeatMap, videoFile: File, outFile: File, rate: Double, delay: Int,
                      windowFrameCount: Int, gameWidth: Int) {
        var ffmpegPath = File("ffmpeg.exe").absolutePath
        debug("FFMPEG path1: ${ffmpegPath}")
        if (!File(ffmpegPath).exists()) {
            ffmpegPath = "ffmpeg"
        }
        debug("FFMPEG path2: ${ffmpegPath}")
        Encoder(beatMap, videoFile, outFile, rate, delay, windowFrameCount, ffmpegPath, gameWidth).generateVideo()
    }

    private val startTimes = hashMapOf<String, Long>()
    private var preProgress = 0.0

    fun printProgress(stage: String, startProgress: Int, endProgress: Int, interProgress: Double) {
        var progress = max(0.0, min(interProgress, 100.0)) / 100 * (endProgress - startProgress) + startProgress
        progress = max(preProgress, progress)
        preProgress = progress
        if (stage !in startTimes.keys) {
            startTimes[stage] = System.currentTimeMillis()
        }
        if (interProgress >= 100) {
            debug("$stage: ${(System.currentTimeMillis() - startTimes[stage]!!) / 1000.0} s")
        }

        if (Config.INSTANCE.isServer) {
            File(Config.INSTANCE.outputDir).resolve("progress.txt").writeText(progress.toString())
        }

        // UI
        if (Config.INSTANCE.isDesktop) {
            println("progress: $progress")
        } else {
            val progressSize = 50
            fun getNChar(count: Int, ch: String) = Collections.nCopies(max(count, 0), ch).joinToString("")
            val solidCount = (progress / 100.0 * progressSize).toInt()
            val voidCount = progressSize - solidCount
            val progressText = String.format("%03.2f%%├", progress)
            val totalCount = solidCount + voidCount + progressText.length + 1

            print(getNChar(totalCount, "\b"))
            print(progressText)

            print(getNChar(solidCount, "█"))
            print(getNChar(voidCount, "-"))
            print("┤")
        }
    }
}