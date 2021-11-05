package me.replaymaster.beatmap

import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.io.File
import java.io.InputStream
import kotlin.math.floor
import kotlin.streams.toList

object OsuMapReader : IMapReader {

    override val mapExtension: String = "osu"
    override val zipExtension: String = "osz"

    override fun readMap(path: String): BeatMap {
        val inputFile = File(path)
        val inputStream: InputStream = inputFile.inputStream()
        var find = false
        var key = -1
        var od = 0.0
        var bgmFile = ""
        val list = inputStream.bufferedReader().lines().filter {
            when {
                it.startsWith("CircleSize") -> {
                    key = it.split(":").last().toInt()
                    logLine("read.beatmap.key", key.toString())
                }
                it.startsWith("OverallDifficulty") -> {
                    od = it.split(":").last().toDouble()
                    logLine("read.beatmap.od", od)
                }
                it.startsWith("AudioFilename") -> {
                    bgmFile = File(inputFile.parent, it.split(":").last().trim()).absolutePath
                    logLine("read.beatmap.bgm", bgmFile)
                }
                it.startsWith("[") -> {
                    find = it == "[HitObjects]"
                    if (find) logLine("read.beatmap.notes")
                    return@filter false
                }
            }
            find
        }
                .map {
                    val elements = it.split(",")
                    val column = floor(elements[0].toInt() * key / 512.0).toInt()
                    val timeStamp = elements[2].toLong()
                    val endTime = elements[5].split(":")[0].toLong()
                    val duration = if (elements[3].toInt() and 128 != 0) endTime - timeStamp else 0L
                    Note(timeStamp, column, duration)
                }
                .toList()
        inputStream.close()
        return BeatMap(key, list.sorted(), bgmFile, od = od)
    }
}