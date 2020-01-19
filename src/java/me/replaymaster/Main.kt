package me.replaymaster

import me.replays.ReplayData
import me.replays.parse.ReplayReader
import org.apache.commons.compress.compressors.CompressorException
import java.io.File

import java.io.IOException
import java.lang.Exception

object Main {

    @Throws(IOException::class, CompressorException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            println("Error argument number: ${args.size}")
            println("Arguments format: [beatmap.osu] [replay.osr]")
            return
        }

        println("Read beatmap: ${args[0]} ... ")
        val beatMap = OsuConverter.fromBeatMap(args[0])
        println("Success!")

        print("Read replay: ${args[1]} ... ")
        val replayData = ReplayData(ReplayReader(File(args[1])).parse())
        println("Success!")

        print("Parse replay ... ")
        replayData.parse()
        val replayNotes = OsuConverter.fromReplay(replayData, beatMap.key)
        println("Success!")

        print("Generate judgement ... ")
        ReplayMaster.judge(beatMap, replayNotes)
        println("Success!")

        println("Begin rendering ...")
        val outFile = File("out.avi")
        val tempFile = File("temp.avi")
        if (outFile.exists()) {
            outFile.delete()
        }
        ReplayMaster.render(beatMap, replayNotes, tempFile.absolutePath)

        println("Begin attaching BGM ...")
        try {
            ReplayMaster.attachBgm(beatMap, tempFile, outFile)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (!outFile.exists()) {
            println("Error: cannot attach BGM to video. Please check if ffmpeg is in the \$PATH\$.")
            tempFile.copyTo(outFile, true)
        }
        println("\nRender success! Output file is: ${outFile.absolutePath}")
        tempFile.delete()
    }
}