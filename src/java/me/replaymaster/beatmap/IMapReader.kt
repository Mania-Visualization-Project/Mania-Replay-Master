package me.replaymaster.beatmap

import me.replaymaster.model.BeatMap
import java.io.File

interface IMapReader {
    val mapExtension: String
    val zipExtension: String

    fun readMap(path: String): BeatMap

    companion object {

        private val TOTAL_MAP_READER_LIST = listOf(OsuMapReader, MalodyMapReader)

        fun isZipMap(beatmapFile: File): Boolean {
            val extension = beatmapFile.extension
            return beatmapFile.extension == "zip" ||
                    TOTAL_MAP_READER_LIST.firstOrNull { it.zipExtension == extension } != null
        }

        fun matchMapReader(beatmapFile: File): IMapReader? {
            val extension = beatmapFile.extension
            return TOTAL_MAP_READER_LIST.firstOrNull { it.mapExtension == extension }
        }
    }
}
