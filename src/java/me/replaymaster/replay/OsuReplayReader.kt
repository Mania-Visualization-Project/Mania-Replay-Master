package me.replaymaster.replay

import me.replaymaster.adjust
import me.replaymaster.debug
import me.replaymaster.judger.OsuManiaJudger
import me.replaymaster.judger.OsuTaikoJudger
import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.ReplayData
import me.replays.parse.ReplayReader
import java.io.File
import kotlin.math.max
import kotlin.math.min


object OsuReplayReader : IReplayReader {

    override val extension = "osr"

    private fun mapDifficultyRange(od: Double, min: Double, mid: Double, max: Double, mod: Int): Double {
        val difficulty = when {
            Mods.has(mod, Mods.HardRock) -> min(10.0, od * 1.4)
            Mods.has(mod, Mods.Easy) -> max(0.0, od / 2)
            else -> od
        }
        val result = when {
            difficulty > 5 -> mid + (max - mid) * (difficulty - 5) / 5
            difficulty < 5 -> mid - (mid - min) * (5 - difficulty) / 5
            else -> mid
        }
        return result.toInt().toDouble()
    }

    // This algorithm may have a error less than 1 ms.
    private fun getJudgement(od: Double, replayData: ReplayData, beatMapType: Int): DoubleArray {
        return if (beatMapType == BeatMap.TYPE_MANIA) {

            val modeRate = when {
                Mods.has(replayData.replay.mods, Mods.HardRock) -> 1.0 / 1.4
                Mods.has(replayData.replay.mods, Mods.Easy) -> 1.4
                else -> 1.0
            }
            doubleArrayOf(16.0, 34.0, 67.0, 97.0, 121.0, 158.0).mapIndexed { index: Int, d: Double ->
                var r = d
                if (index != 0) {
                    r += 3 * (10 - od)
                }
                (r * modeRate).toInt().toDouble()
            }.toDoubleArray()

        } else {
            doubleArrayOf(
                    mapDifficultyRange(od, 50.0, 35.0, 20.0, replayData.replay.mods),
                    mapDifficultyRange(od, 120.0, 80.0, 50.0, replayData.replay.mods),
                    mapDifficultyRange(od, 135.0, 95.0, 70.0, replayData.replay.mods)
            )
        }
    }

    override fun getJudger(beatMap: BeatMap, replayModel: ReplayModel) = when (beatMap.type) {
        BeatMap.TYPE_MANIA -> OsuManiaJudger(beatMap, replayModel)
        BeatMap.TYPE_TAIKO -> OsuTaikoJudger(beatMap, replayModel)
        else -> error("")
    }

    override fun readReplay(path: String, beatMap: BeatMap): ReplayModel {

        val replayData = ReplayData(ReplayReader(File(path)).parse())
        debug("Judgement results in replay: ${replayData.replay.beat300}, ${replayData.replay.hit300}, " +
                "${replayData.replay.beat100}, ${replayData.replay.hit100}, ${replayData.replay.hit50}, " +
                "${replayData.replay.misses}")
        replayData.parse()

        var current: Long = 0
        val holdBeginTime = Array<Long>(beatMap.key) { 0 }
        val list = arrayListOf<Note>()
        for (i in 0..(replayData.actions.size - 2)) {
            val action = replayData.actions[i]
            var x = when (beatMap.type) {
                BeatMap.TYPE_MANIA -> action.x.toInt()
                BeatMap.TYPE_TAIKO -> action.z
                else -> error("")
            }
            current += action.w
            for (j in 0 until beatMap.key) {
                val column = when (beatMap.type) {
                    BeatMap.TYPE_MANIA -> j
                    BeatMap.TYPE_TAIKO -> if (j == 0) 1 else if (j == 1) 0 else j
                    else -> error("")
                }
                if (x and 1 != 0) {
                    if (holdBeginTime[column] == 0L) holdBeginTime[column] = current
                } else {
                    if (holdBeginTime[column] != 0L) {
                        list.add(
                                Note(holdBeginTime[column], column, current - holdBeginTime[column])
                        )
                        holdBeginTime[column] = 0L
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

        val judgement = getJudgement(beatMap.od, replayData, beatMap.type)
        logLine("read.beatmap.judgement", judgement.contentToString())

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