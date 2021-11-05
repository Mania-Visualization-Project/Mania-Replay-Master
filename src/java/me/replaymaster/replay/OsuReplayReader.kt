package me.replaymaster.replay

import me.replaymaster.adjust
import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.ReplayData
import me.replays.parse.ReplayReader
import java.io.File
import java.util.*


object OsuReplayReader : IReplayReader {

    override val extension = "osr"

    private val BASE_JUDGEMENT_OFFSET = doubleArrayOf(16.5, 64.5, 97.5, 127.5, 151.5, 188.5)
    private val BASE_JUDGEMENT_OFFSET_HR = doubleArrayOf(11.5, 45.5, 69.5, 90.5, 107.5, 133.5)
    private val BASE_JUDGEMENT_OFFSET_EZ = doubleArrayOf(22.5, 89.5, 135.5, 177.5, 211.5, 263.5)

    private val DECREMENT_NONE = 3.0
    private val DECREMENT_HR = 2.1
    private val DECREMENT_EZ = 4.2

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

    override fun readReplay(path: String, beatMap: BeatMap): ReplayModel {

        val replayData = ReplayData(ReplayReader(File(path)).parse())
        replayData.parse()

        var current: Long = 0
        val holdBeginTime = Array<Long>(beatMap.key) { 0 }
        val list = arrayListOf<Note>()
        for (i in 0..(replayData.actions.size - 2)) {
            val action = replayData.actions[i]
            var x = action.x.toInt()
            current += action.w
            for (j in 0..(beatMap.key - 1)) {
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
        logLine("parse.replay.rate", rate)

        val judgement = getJudgement(beatMap.od, replayData)
        logLine("read.beatmap.judgement", Arrays.toString(judgement))

        val mirror = replayData.replay.mods and (1 shl 30) != 0
        val replayModel = ReplayModel(list, rate, mirror, judgement)
        adjust(beatMap, replayModel)


        return replayModel
    }

    override fun readBeatMapMD5(replayPath: String): String {
        val osuReplay = ReplayReader(File(replayPath)).parse()
        return osuReplay.beatmapHash
    }
}