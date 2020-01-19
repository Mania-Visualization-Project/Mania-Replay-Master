package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.ReplayData
import java.io.File
import java.io.InputStream
import kotlin.math.floor
import kotlin.streams.toList

object OsuConverter {

    private val BASE_JUDGEMENT_OFFSET = doubleArrayOf(16.5, 64.5, 97.5, 127.5, 151.5)
    private val BASE_JUDGEMENT_OFFSET_DT = doubleArrayOf(16.33, 64.33, 97.0, 127.0, 151.0)
    private val BASE_JUDGEMENT_OFFSET_HT = doubleArrayOf(16.67, 64.67, 96.67, 127.33, 151.33)

    private fun getJudgement(od: Double, replayData: ReplayData): DoubleArray {
        val result = when {
            Mods.has(replayData.replay.mods, Mods.DoubleTime) -> BASE_JUDGEMENT_OFFSET_DT
            Mods.has(replayData.replay.mods, Mods.HalfTime) -> BASE_JUDGEMENT_OFFSET_HT
            else -> BASE_JUDGEMENT_OFFSET
        }.copyOf()
        for (i in 1..4) {
            result[i] -= 3 * od
        }
        return result
    }

    fun fromBeatMap(path: String, replayData: ReplayData): BeatMap {
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
                    if (find) println(">>>> read notes ...")
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
        return BeatMap(key, getJudgement(od, replayData), list.sorted(), bgmFile)
    }

    fun scaleRate(replayModel: ReplayModel, beatMap: BeatMap) {
        val rate = replayModel.rate
        replayModel.replayData.forEach {
            it.scale(rate)
        }
        beatMap.notes.forEach {
            it.scale(rate)
        }
    }

    fun fromReplay(replayData: ReplayData, key: Int): ReplayModel {
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
        val rate = when {
            Mods.has(replayData.replay.mods, Mods.DoubleTime) -> 1.5
            Mods.has(replayData.replay.mods, Mods.HalfTime) -> 0.75
            else -> 1.0
        }
        println(">>>> read rate: x$rate")
        return ReplayModel(list, rate)
    }
}