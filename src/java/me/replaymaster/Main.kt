package me.replaymaster

import me.replays.ReplayData
import me.replays.parse.ReplayReader
import org.apache.commons.compress.compressors.CompressorException
import java.io.File

import java.io.IOException

object Main {

    val BEAT_MAP = "C:\\Users\\22428\\Desktop\\1061136 osu!mania 7K Dan Course - Dan Phase IV\\osu!mania 7K Dan Course - Dan Phase IV (Jinjin) [Stellium Dan (Left)].osu"
    val REPLAY = "G:\\E\\CaOH2 - osu!mania 7K Dan Course - Dan Phase IV [Stellium Dan (Left 1234)] (2019-12-24) OsuMania.osr"
    @Throws(IOException::class, CompressorException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val beatMap = OsuConverter.fromBeatMap(BEAT_MAP)
        val replayData = ReplayData(ReplayReader(File(REPLAY)).parse())
        replayData.parse()
        val replayNotes = OsuConverter.fromReplay(replayData, beatMap.key)
        ReplayMaster.judge(beatMap, replayNotes)
        ReplayMaster.render(beatMap, replayNotes, File("out.avi").absolutePath)
    }
}