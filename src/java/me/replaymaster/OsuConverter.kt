package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replays.ReplayData
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.floor
import kotlin.streams.toList

object OsuConverter {

    private val BASE_JUDGEMENT_OFFSET = doubleArrayOf(16.5, 64.5, 97.5, 127.5, 151.5)

    private fun getJudgement(od: Double): DoubleArray {
        val result = BASE_JUDGEMENT_OFFSET.copyOf()
        for (i in 1..4) {
            result[i] -= 3 * od
        }
        return result
    }

    fun fromBeatMap(path: String): BeatMap {
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
                    println(">>>> read key: $key")
                }
                it.startsWith("OverallDifficulty") -> {
                    od = it.split(":").last().toDouble()
                    println(">>>> read od: $od")
                }
                it.startsWith("AudioFilename") -> {
                    bgmFile = File(inputFile.parent, it.split(":").last().trim()).absolutePath
                    println(">>>> read bgm: $bgmFile")
                }
                it.startsWith("[") -> {
                    find = it == "[HitObjects]"
                    return@filter false
                }
            }
            find
        }
                .map {
                    val elements = it.split(",")
                    val column = floor(elements[0].toInt() * key / 512.0).toInt()
                    val timeStamp = elements[2].toLong()
                    val endTime = elements[4].toInt()
                    val duration = if (endTime != 0) endTime - timeStamp else 0
                    Note(timeStamp, column, duration)
                }
                .toList()
        inputStream.close()
        return BeatMap(key, getJudgement(od), list.sorted(), bgmFile)
    }

    fun fromReplay(replayData: ReplayData, key: Int): List<Note> {
        var current: Long = 0
        val holdBeginTime = Array<Long>(key) { 0 }
        val list = arrayListOf<Note>()
        for (i in 2..(replayData.actions.size - 2)) {
            val action = replayData.actions[i]
            var x = action.x.toInt()
            current += action.w
            for (j in 0..(key - 1)) {
                if (x and 1 != 0) {
                    if (holdBeginTime[j] == 0L) holdBeginTime[j] = current
                } else {
                    if (holdBeginTime[j] != 0L) {
                        list.add(
                                Note(holdBeginTime[j], j, current - holdBeginTime[j])
                        )
                        holdBeginTime[j] = 0L
                    }
                }
                x /= 2
            }
        }
        list.sort()
        return list
    }
}