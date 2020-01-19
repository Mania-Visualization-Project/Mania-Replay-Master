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
        val inputStream: InputStream = File(path).inputStream()
        var find = false
        var key = -1
        var od = 0.0
        val list = inputStream.bufferedReader().lines().filter {
            when {
                it.startsWith("CircleSize") -> key = it.split(":").last().toInt()
                it.startsWith("OverallDifficulty") -> od = it.split(":").last().toDouble()
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
        return BeatMap(key, getJudgement(od), list.sorted())
    }

    fun fromReplay(replayData: ReplayData, key: Int): List<Note> {
        val fos = FileOutputStream(File("left.txt"))
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
//                fos.write("$x $j ${x and 1} $current\n".toByteArray())
                x /= 2
            }
        }
        list.sort()
        fos.close()
        return list
    }
}