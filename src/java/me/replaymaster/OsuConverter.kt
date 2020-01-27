package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.ReplayData
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.math.floor
import kotlin.streams.toList

object OsuConverter {

    private val BASE_JUDGEMENT_OFFSET = doubleArrayOf(16.5, 64.5, 97.5, 127.5, 151.5, 188.5)
    private val BASE_JUDGEMENT_OFFSET_HR = doubleArrayOf(11.5, 45.5, 69.5, 90.5, 107.5, 133.5)
    private val BASE_JUDGEMENT_OFFSET_EZ = doubleArrayOf(22.5, 89.5, 135.5, 177.5, 211.5, 263.5)

    private val DECREMENT_NONE = 3.0
    private val DECREMENT_HR = 2.1
    private val DECREMENT_EZ = 4.2

    // FIXME: this is not the exact way the judgement time is produced. PPY has a strange algorithm that I don't know.
    // This algorithm may have a error less than 1 ms.
    private fun getJudgement(od: Double, replayData: ReplayData): DoubleArray {
        val result = when {
            Mods.has(replayData.replay.mods, Mods.HardRock) -> BASE_JUDGEMENT_OFFSET_HR
            Mods.has(replayData.replay.mods, Mods.Easy) -> BASE_JUDGEMENT_OFFSET_EZ
            else -> BASE_JUDGEMENT_OFFSET
        }.copyOf()
        val decrement = when {
            Mods.has(replayData.replay.mods, Mods.HardRock) -> DECREMENT_HR
            Mods.has(replayData.replay.mods, Mods.Easy) -> DECREMENT_EZ
            else -> DECREMENT_NONE
        }
        for (i in 1..result.lastIndex) {
            result[i] -= decrement * od
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
                    println(Main.RESOURCE_BUNDLE.getFormatString("read.beatmap.key", key.toString()))
                }
                it.startsWith("OverallDifficulty") -> {
                    od = it.split(":").last().toDouble()
                    println(Main.RESOURCE_BUNDLE.getFormatString("read.beatmap.od", od))
                }
                it.startsWith("AudioFilename") -> {
                    bgmFile = File(inputFile.parent, it.split(":").last().trim()).absolutePath
                    println(Main.RESOURCE_BUNDLE.getFormatString("read.beatmap.bgm", bgmFile))
                }
                it.startsWith("[") -> {
                    find = it == "[HitObjects]"
                    if (find) println(Main.RESOURCE_BUNDLE.getString("read.beatmap.notes"))
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
        val judgement = getJudgement(od, replayData)
        println(Main.RESOURCE_BUNDLE.getFormatString("read.beatmap.judgement", Arrays.toString(judgement)))
        val judgementEnd = judgement.map { it * 1.5 }.toDoubleArray()
        return BeatMap(key, judgement, list.sorted(), bgmFile, judgementEnd)
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

    fun mirror(beatMap: BeatMap) {
        beatMap.notes.forEach {
            it.column = beatMap.key - it.column - 1
        }
    }

    fun fromReplay(replayData: ReplayData, key: Int): ReplayModel {
        var current: Long = 0
        val holdBeginTime = Array<Long>(key) { 0 }
        val list = arrayListOf<Note>()
        for (i in 0..(replayData.actions.size - 2)) {
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
        println(Main.RESOURCE_BUNDLE.getFormatString("parse.replay.rate", rate))
        val mirror = replayData.replay.mods and (1 shl 30) != 0
        return ReplayModel(list, rate, mirror)
    }
}