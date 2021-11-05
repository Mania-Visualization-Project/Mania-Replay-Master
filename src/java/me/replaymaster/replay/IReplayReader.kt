package me.replaymaster.replay

import me.replaymaster.model.BeatMap
import me.replaymaster.model.ReplayModel
import java.io.File

interface IReplayReader {
    val extension: String
    fun readBeatMapMD5(replayPath: String): String
    fun readReplay(path: String, beatMap: BeatMap): ReplayModel

    companion object {

        private val TOTAL_REPLAY_READER_LIST = listOf(OsuReplayReader, MalodyReplayReader)

        fun matchReplayReader(replayFile: File): IReplayReader? {
            val extension = replayFile.extension
            return TOTAL_REPLAY_READER_LIST.firstOrNull { it.extension == extension }
        }
    }
}