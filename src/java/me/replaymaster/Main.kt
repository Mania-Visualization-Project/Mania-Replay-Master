package me.replaymaster


import com.google.gson.Gson
import me.replaymaster.beatmap.IMapReader
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.ReplayModel
import me.replaymaster.replay.IReplayReader
import org.apache.commons.compress.compressors.CompressorException
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.system.exitProcess

object Main {

    var judgementFromReplay = listOf<Int>()
    var judgementFromJudging = listOf<Int>()

    private fun prepareJudgements(beatMapFileParam: File, replayFile: File, parent: File, tempDir: File): Triple<BeatMap, ReplayModel, Long> {
        var beatMapFile = beatMapFileParam
        val replayReader = checkNotNull(IReplayReader.matchReplayReader(replayFile)) {
            "Invalid replay file: ${replayFile.absolutePath}"
        }
        val md5 = try {
            replayReader.readBeatMapMD5(replayFile.absolutePath).toUpperCase()
        } catch (t: Throwable) {
            throw IllegalArgumentException("Invalid replay file: ${replayFile.absolutePath}", t)
        }
        if (!beatMapFile.isDirectory && IMapReader.isZipMap(beatMapFile)) {
            val unzipPath = tempDir.resolve("temp_map")
            unzipPath.mkdir()
            logLine("read.beatmap.iscompressed", unzipPath.absolutePath)
            unzip(beatMapFile.absolutePath, unzipPath.absolutePath)
            beatMapFile = unzipPath
        }
        if (beatMapFile.isDirectory) {
            logLine("read.beatmap.dir", beatMapFile.absolutePath)
            val candidates = beatMapFile.walk().filter {
                IMapReader.matchMapReader(it) != null && !it.isDirectory
            }.toList()
            beatMapFile = if (candidates.size == 1) {
                candidates[0]
            } else {
                checkNotNull(candidates.firstOrNull {
                    checkMD5(it, md5)
                }) { "Cannot find the beatmap with the given replay file with MD5: $md5" }
            }
        }
        if (!checkMD5(beatMapFile, md5)) {
            warning("warning.md5", md5)
        }

        logLine("read.beatmap", beatMapFile.path)
        val beatMap = try {
            IMapReader.matchMapReader(beatMapFile)!!.readMap(beatMapFile.absolutePath)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Invalid beatmap file: ${beatMapFile.absolutePath}", t)
        }
        beatMap.checkValidation()

        logLine("read.replay", replayFile.path)
        val replayModel = try {
            replayReader.readReplay(replayFile.absolutePath, beatMap)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Invalid replay file: ${replayFile.absolutePath}", t)
        }
        beatMap.duration = maxOf(replayModel.replayData.last().endTime, beatMap.notes.last().endTime) + 2000L

        logLine("judgement.generate")
        val judger = replayReader.getJudger(beatMap, replayModel)
        judger.judge()

        if (Config.INSTANCE.exportJudgementResults) {
            val exportFile = parent.resolve("judgement_${replayFile.nameWithoutExtension}.json")
            logLine("judgement.export", exportFile.absolutePath)
            exportFile.writeText(Gson().toJson(replayModel.replayData))
        }

        val delay = 1000L
        beatMap.notes.forEach { it.timeStamp += delay }
        replayModel.replayData.forEach { it.timeStamp += delay }

        return Triple(beatMap, replayModel, delay)
    }

    @Throws(IOException::class, CompressorException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val yamlPath = if (args.size > 2) {
            readFile(args[2])
        } else {
            File("config.txt")
        }

        Config.init(yamlPath)
        printWelcome()

        logLine("config.hint", yamlPath.absolutePath)
        println()

        val scanner = Scanner(System.`in`)
        val beatMapFile = if (args.isNotEmpty()) {
            readFile(args[0])
        } else {
            logLine("hint.beatmap")
            readFile(scanner.nextLine())
        }

        val replayFile = if (args.size > 1) {
            readFile(args[1])
        } else {
            logLine("hint.replay")
            readFile(scanner.nextLine())
        }

        debug("beatmap: $beatMapFile, replay: $replayFile")
        val parent = File(Config.INSTANCE.outputDir)
        Config.refresh(yamlPath)
        val startTime = System.currentTimeMillis()

        try {
            // workspace
            val tempDir = parent.resolve("temp_dir")
            if (!parent.isDirectory) {
                parent.mkdir()
            }
            if (tempDir.isDirectory) {
                tempDir.deleteRecursively()
            }
            tempDir.mkdir()

            // judgement
            val (beatMap, replayModel, delay) = prepareJudgements(beatMapFile, replayFile, parent, tempDir)

            // render
            logLine("render", Config.INSTANCE.speed)
            val outFile = File(parent, "${replayFile.nameWithoutExtension}.mp4")
            val tempFile = File(tempDir, "${replayFile.nameWithoutExtension}.png")
            if (outFile.exists()) {
                outFile.delete()
            }
            val windowFrameCount = Render(beatMap, replayModel, tempFile).start()

            // bgm
            try {
                ReplayMaster.generateVideo(beatMap, tempFile, outFile, replayModel.rate, delay.toInt(), windowFrameCount)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            // finish
            check(outFile.exists()) { "generated file failed!" }

            logLine("render.success", outFile.absolutePath)
            if (Config.INSTANCE.isDesktop) {
                println("out: ${outFile.absolutePath}")
            }

            Monitor.reportTask(startTime, beatMapFile, replayFile, File(beatMap.bgmPath), "")

            if (!Config.INSTANCE.debug) {
                tempDir.deleteRecursively()
            }

            if (!Config.INSTANCE.isServer && !Config.INSTANCE.isDesktop) {
                scanner.nextLine()
                Desktop.getDesktop().open(outFile)
            }
        } catch (throwable: Throwable) {
            if (Config.INSTANCE.isServer) {
//                parent.resolve("error.txt").writeText(StringWriter().apply {
//                    throwable.printStackTrace(PrintWriter(this))
//                }.toString())
                parent.resolve("error.txt").writeText(throwable.toString() + "\nerror code: " + parseExceptionCode(throwable))
            }

            Monitor.reportTask(startTime, beatMapFile, replayFile, null, StringWriter().apply {
                throwable.printStackTrace(PrintWriter(this))
            }.toString())

            if (Config.INSTANCE.isDesktop) {
                println("error: ${parseExceptionCode(throwable)}")
                exitProcess(1)
            }

            throw throwable
        }
    }

    private fun printWelcome() {
        if (Config.INSTANCE.isDesktop) {
            return
        }
        val reposite = RESOURCE_BUNDLE.getString("app.reposite")
        val appName = RESOURCE_BUNDLE.getString("app.name")
        val versionName = RESOURCE_BUNDLE.getString("app.version")
        val authorName = RESOURCE_BUNDLE.getString("app.author")
        val length = reposite.length + 2
        val header = "=".repeat(length)

        println(header)
        printItem("$appName $versionName", length)
        printItem(authorName, length)
        printItem(reposite, length)
        printItem(header, length)
    }

    private fun printItem(content: String, length: Int) {
        val space = " ".repeat((length - content.length) / 2)
        print(space)
        print(content)
        println(space)
    }

    private fun readFile(content: String): File {
        val c = content.trim()
        if (c.startsWith("\"") && c.endsWith("\"")) {
            return File(c.substring(1, c.length - 1))
        }
        return File(c)
    }

    private fun checkMD5(beatMapFile: File, md5: String): Boolean {
        return getMD5(beatMapFile)?.toUpperCase() == md5
    }

    private fun parseExceptionCode(throwable: Throwable): String {
        val msg = throwable.toString()
        return when {
            "enerated file fail" in msg -> {
                "render_failed"
            }

            "Cannot find the beatmap with the given" in msg -> {
                "beatmap_not_found"
            }

            "Invalid beatmap file" in msg || "Invalid .mc file" in msg -> {
                "beatmap_invalid"
            }

            "Invalid replay file" in msg || "not a valid .mr file" in msg -> {
                "replay_invalid"
            }

            else -> msg
        }
    }
}