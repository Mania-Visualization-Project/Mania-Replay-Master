package me.replaymaster


import com.google.gson.Gson
import me.replaymaster.beatmap.IMapReader
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.ReplayModel
import me.replaymaster.replay.IReplayReader
import java.awt.Desktop
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import kotlin.system.exitProcess

object Main {

    var judgementFromReplay = listOf<Int>()
    var judgementFromJudging = listOf<Int>()

    private fun prepareJudgements(beatMapFileParam: File, replayFile: File, parent: File, tempDir: File): Triple<BeatMap, ReplayModel, Long> {
        var beatMapFile = beatMapFileParam
        val replayReader = IReplayReader.matchReplayReader(replayFile)
                ?: throw InvalidReplayException(replayFile.absolutePath, Exception())
        val md5 = try {
            replayReader.readBeatMapMD5(replayFile.absolutePath).toUpperCase()
        } catch (t: Throwable) {
            throw InvalidReplayException(replayFile.absolutePath, t)
        }
        if (!beatMapFile.isDirectory && IMapReader.isZipMap(beatMapFile)) {
            val unzipPath = tempDir.resolve("temp_map")
            unzipPath.mkdir()
            logLine("read.beatmap.iscompressed", unzipPath.absolutePath)
            try {
                unzip(beatMapFile.absolutePath, unzipPath.absolutePath)
            } catch (t: Throwable) {
                throw InvalidBeatmapException(beatMapFile.absolutePath, t)
            }
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
                candidates.firstOrNull {
                    checkMD5(it, md5)
                } ?: throw BeatmapNotFoundException(md5)
            }
        }
        if (!checkMD5(beatMapFile, md5)) {
            warning("warning.md5", md5)
        }

        logLine("read.beatmap", beatMapFile.path)
        val beatMap = try {
            IMapReader.matchMapReader(beatMapFile)!!.readMap(beatMapFile.absolutePath)
        } catch (t: Throwable) {
            if (t is BaseException) {
                throw t
            }
            throw InvalidBeatmapException(beatMapFile.absolutePath, t)
        }
        beatMap.checkValidation()
        if (Config.INSTANCE.isServer) {
            parent.resolve("game_mode.txt").writeText(beatMap.gameMode)
        }

        logLine("read.replay", replayFile.path)
        val replayModel = try {
            replayReader.readReplay(replayFile.absolutePath, beatMap)
        } catch (t: Throwable) {
            if (t is BaseException) {
                throw t
            }
            throw InvalidReplayException(replayFile.absolutePath, t)
        }
        val delay = 1000L
        beatMap.notes.forEach { it.timeStamp += delay }
        replayModel.replayData.forEach { it.timeStamp += delay }
        beatMap.duration = maxOf(replayModel.replayData.last().endTime, beatMap.notes.last().endTime) + 2000L

        logLine("judgement.generate")
        val judger = replayReader.getJudger(beatMap, replayModel)
        judger.judge()

        if (Config.INSTANCE.exportJudgementResults) {
            val exportFile = parent.resolve("judgement_${replayFile.nameWithoutExtension}.json")
            logLine("judgement.export", exportFile.absolutePath)
            exportFile.writeText(Gson().toJson(replayModel.replayData))
        }

        return Triple(beatMap, replayModel, delay)
    }

    @JvmStatic
    fun main(args: Array<String>) {

        val yamlPath = if (args.size > 2) {
            readFile(args[2])
        } else {
            File("config.txt")
        }

        Config.init(yamlPath)
        printWelcome()
        UpdateHelper.checkUpdate()

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

            UpdateHelper.notifyLatestVersionIfNecessary()

            // render
            logLine("render", Config.INSTANCE.speed)
            val outFile = File(parent, "${replayFile.nameWithoutExtension}.mp4")
            val tempFile = File(tempDir, "${replayFile.nameWithoutExtension}.png")
            if (outFile.exists()) {
                outFile.delete()
            }

            try {
                val windowFrameCount = Render(beatMap, replayModel, tempFile).start()
                ReplayMaster.generateVideo(beatMap, tempFile, outFile, replayModel.rate, delay.toInt(), windowFrameCount)
            } catch (ex: Exception) {
                throw VideoGenerationException(ex)
            }

            // finish
            if (!outFile.exists()) {
                throw VideoGenerationException(Exception())
            }

            logLine("render.success", outFile.absolutePath)
            if (Config.INSTANCE.isDesktop) {
                println("out: ${outFile.absolutePath}")
            }

            Monitor.reportTask(startTime, beatMapFile, replayFile, File(beatMap.bgmPath), "", beatMap.gameMode)

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
            }.toString(), "")

            if (Config.INSTANCE.isDesktop) {
                println("error: ${parseExceptionCode(throwable)}")
                exitProcess(1)
            }

            println("\nError: ${throwable.message}")
            exitProcess(1)
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
        return if (throwable is BaseException) {
            throwable.errorCode
        } else {
            throwable.toString()
        }
    }
}