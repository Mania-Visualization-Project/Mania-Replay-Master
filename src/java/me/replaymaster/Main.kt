package me.replaymaster

import me.replays.ReplayData
import me.replays.parse.ReplayReader
import org.apache.commons.compress.compressors.CompressorException
import java.io.File

import java.io.IOException
import java.lang.Exception
import java.util.*

object Main {

    val RESOURCE_BUNDLE = ResourceBundle.getBundle("res/language", Utf8Control())!!

    @Throws(IOException::class, CompressorException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            onErrorParams(args.size)
        }

        var speed = 15
        var paramStart = 0

        if (args[paramStart].startsWith("-speed=")) {
            speed = args[paramStart].split("=", limit = 2)[1].toInt()
            paramStart += 1
        }

        val beatMapFile = readFile(args[paramStart])
        val replayFile = readFile(args[paramStart + 1])

        printWelcome()

        print(RESOURCE_BUNDLE.getFormatString("read.replay", replayFile))
        val replayData = ReplayData(ReplayReader(File(replayFile)).parse())
        replayData.parse()
        println(RESOURCE_BUNDLE.getString("success"))

        println(RESOURCE_BUNDLE.getFormatString("read.beatmap", beatMapFile))
        val beatMap = OsuConverter.fromBeatMap(beatMapFile, replayData)
        println(RESOURCE_BUNDLE.getString("success"))

        println(RESOURCE_BUNDLE.getString("parse.replay"))
        val replayModel = OsuConverter.fromReplay(replayData, beatMap.key)
        println(RESOURCE_BUNDLE.getString("success"))

        if (replayModel.rate != 1.0) {
            print(RESOURCE_BUNDLE.getFormatString("rate.scale", replayModel.rate))
            OsuConverter.scaleRate(replayModel, beatMap)
            println(RESOURCE_BUNDLE.getString("success"))
        }

        print(RESOURCE_BUNDLE.getString("judgement.generate"))
        ReplayMaster.judge(beatMap, replayModel, true)
        println(RESOURCE_BUNDLE.getString("success"))

        println(RESOURCE_BUNDLE.getFormatString("render", speed))
        val outFile = File("out.avi")
        val tempFile = File("temp.avi")
        if (outFile.exists()) {
            outFile.delete()
        }
        ReplayMaster.render(beatMap, replayModel, tempFile.absolutePath, speed)

        println(RESOURCE_BUNDLE.getString("attach.bgm"))
        try {
            ReplayMaster.attachBgm(beatMap, tempFile, outFile, replayModel.rate)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (!outFile.exists()) {
            println("Error: cannot attach BGM to video. Please check if ffmpeg is in the \$PATH\$.")
            tempFile.copyTo(outFile, true)
        }

        println(RESOURCE_BUNDLE.getFormatString("render.success", outFile.absolutePath))
        tempFile.delete()
    }

    private fun printWelcome() {
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

    private fun readFile(content: String): String {
        if (content.startsWith("\"") && content.endsWith("\"")) {
            return content.substring(1, content.length - 1)
        }
        return content
    }

    private fun onErrorParams(argsSize: Int) {
        println(RESOURCE_BUNDLE.getFormatString("error.argument", argsSize))
        println(RESOURCE_BUNDLE.getString("error.argument.hint"))
        System.exit(-1)
    }
}

fun ResourceBundle.getFormatString(key: String, vararg args: Any?): String {
    return String.format(getString(key), *args)
}