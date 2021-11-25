package me.replaymaster.replay

import me.replaymaster.adjust
import me.replaymaster.debug
import me.replaymaster.judger.OsuJudger
import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.ReplayData
import me.replays.parse.ReplayReader
import java.io.File


object OsuReplayReader : IReplayReader {

    override val extension = "osr"

    private val BASE_JUDGEMENT = doubleArrayOf(16.0, 34.0, 67.0, 97.0, 121.0, 158.0)

    // This algorithm may have a error less than 1 ms.
    private fun getJudgement(od: Double, replayData: ReplayData): DoubleArray {
        val modeRate = when {
            Mods.has(replayData.replay.mods, Mods.HardRock) -> 1.0 / 1.4
            Mods.has(replayData.replay.mods, Mods.Easy) -> 1.4
            else -> 1.0
        }
        val result = BASE_JUDGEMENT.mapIndexed { index: Int, d: Double ->
            var r = d
            if (index != 0) {
                r += 3 * (10 - od)
            }
            (r * modeRate).toInt().toDouble()
        }
        return result.toDoubleArray()
    }

    override fun getJudger(beatMap: BeatMap, replayModel: ReplayModel) = OsuJudger(beatMap, replayModel)

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
            var x = action.x.toInt()
            current += action.w
            for (j in 0 until beatMap.key) {
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