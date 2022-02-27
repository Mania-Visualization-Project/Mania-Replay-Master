package me.replaymaster.judger

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import kotlin.math.abs

class OsuManiaJudger(
        beatMap: BeatMap,
        replayModel: ReplayModel
) : BaseJudger(beatMap, replayModel) {

    companion object {
        private const val J_MAX = 0
        private const val J_300 = 1
        private const val J_200 = 2
        private const val J_100 = 3
        private const val J_50 = 4
        private const val J_MISS = 5
    }

    private val LNHasJudged = HashSet<Note>()

    override fun canHit(action: Note, note: Note): Int {
        if (action.column != note.column) {
            return IGNORE
        }
        var diff = action.timeStamp - note.timeStamp
        if (-diff > judgementWindow[J_MISS]) {
            return TOO_EARLY
        }
        var isTooLate: Boolean = false
        if (note.duration != 0L) {
            isTooLate = action.timeStamp - note.endTime > judgementWindow[J_100] * if (replayModel.isScoreV2) 1.5 else 1.0
            if (Config.INSTANCE.omScoreV2Adjust) {
                var nextNote: Note? = null
                var i = note.index + 1
                while (i < beatMap.notes.size) {
                    if (beatMap.notes[i].column == note.column) {
                        nextNote = beatMap.notes[i]
                        break
                    }
                    i += 1
                }
                if (nextNote != null) {
                    val nextNoteDiff = nextNote.timeStamp - note.timeStamp
                    isTooLate = action.timeStamp > (when {
                        nextNoteDiff < judgementWindow[J_50] -> nextNote.timeStamp - 1
                        nextNoteDiff > judgementWindow[J_50] * 2 -> nextNote.timeStamp - judgementWindow[J_50]
                        else -> note.timeStamp + judgementWindow[J_50]
                    }).toDouble()
                }
            }
        } else {
            isTooLate = diff >= judgementWindow[J_100]
        }
        if (isTooLate)
            return TOO_LATE
        else if (note.duration == 0L)
            return HIT
        else {
            var i = action.index
            while (i >= 1) {
                i -= 1
                val anotherAction = replayModel.replayData[i]
                if (anotherAction.column != action.column) {
                    continue
                }
                val endTime = anotherAction.endTime
                if (endTime < note.endTime - judgementWindow[J_50]) {
                    break
                }
                val isHoldTooLong = anotherAction.endTime > (anotherAction.relatedActionOrNote?.endTime ?: 0) + judgementWindow[J_100] * if (replayModel.isScoreV2) 1.5 else 1.0
                if (!isHoldTooLong && anotherAction.relatedActionOrNote != null) {
                    continue
                }
                if (endTime < note.endTime + judgementWindow[J_100] * (if (replayModel.isScoreV2) 1.5 else 1.0) && endTime > note.timeStamp + judgementWindow[J_50]) {
                    return TOO_LATE
                }
            }
            return HIT_AND_CAN_HIT_AGAIN
        }
    }

    override fun getJudgement(diff: Double, note: Note?, action: Note?): Int {
        if (note != null && replayModel.isScoreV2) {
//            if (LNHasJudged.contains(note)) {
//                return note.judgementStart
//            }
        }
        return super.getJudgement(diff, note, action)
    }

    override fun calculateAccuracy(notes: List<Note>): Double {
        return if (!replayModel.isScoreV2) {
            notes.map {
                when (it.judgementStart) {
                    J_MAX -> 1.0
                    J_300 -> 1.0
                    J_200 -> 2.0 / 3.0
                    J_100 -> 1.0 / 3.0
                    J_50 -> 0.5 / 3.0
                    else -> 0.0
                }
            }.average()
        } else {
            val accs = mutableListOf<Double>()

            fun judgementToAcc(judgement: Int) = when (judgement) {
                J_MAX -> 305.0 / 305
                J_300 -> 300.0 / 305
                J_200 -> 200.0 / 305
                J_100 -> 100.0 / 305
                J_50 -> 50.0 / 305
                else -> 0.0
            }

            notes.forEach {
                accs.add(judgementToAcc(it.judgementStart))
                if (it.duration != 0L) {
                    accs.add(judgementToAcc(it.judgementEnd))
                }
            }

            accs.average()
        }
    }

    private fun isLNJudgedWith(startDiff: Long, totalDiff: Long, judgement: Int, rate: Double): Boolean {
        val judgementTime = judgementWindow[judgement] * rate
        return startDiff <= judgementTime && totalDiff <= judgementTime * 2
    }

    override fun judgeRelease(action: Note, target: Note): Boolean {
        var endDiff = abs(action.endTime - target.endTime)
        val signedEndDiff = action.endTime - target.endTime

        // judge end (Score V2)
        action.judgementEnd = if (signedEndDiff <= judgementWindow[J_100] * 1.5) {
            getJudgement(endDiff / 1.5)
        } else {
            -1
        }
//        action.judgementEnd = if (action.endTime - target.endTime <= judgementWindow[J_100] * 1.5) {
//            getJudgement(endDiff / 1.5)
//        } else {
//            -1
//        }
        if (LNHasJudged.contains(target) || target.judgementStart == -1 || target.judgementStart == J_MISS) {
            if (action.judgementEnd >= J_MAX && action.judgementEnd <= J_100) {
                action.judgementEnd = J_50
            }
        }
        target.judgementEnd = action.judgementEnd

        // adjust target's judgement
        var startDiff = abs(action.timeStamp - target.timeStamp)
        if (target.timeStamp - judgementWindow[J_50] > action.timeStamp) {
            // fxxking ppy
            val start = target.endTime - 1
            startDiff = abs(target.timeStamp - start)
        }
        val totalDiff = startDiff + endDiff

        var totalJudgement: Int
        val canBeJudgedAgain: Boolean
        if (action.endTime - target.endTime < -judgementWindow[J_50]) {
            totalJudgement = -1
            canBeJudgedAgain = true
        } else {
            canBeJudgedAgain = false
            totalJudgement = when {
                isLNJudgedWith(startDiff, totalDiff, J_MAX, 1.2) -> J_MAX
                isLNJudgedWith(startDiff, totalDiff, J_300, 1.1) -> J_300
                isLNJudgedWith(startDiff, totalDiff, J_200, 1.0) -> J_200
                isLNJudgedWith(startDiff, totalDiff, J_100, 1.0) -> J_100
                else -> J_50
            }
        }
        if (LNHasJudged.contains(target) && (totalJudgement == J_MAX || totalJudgement == J_300)) {
            totalJudgement = J_200
        }
        LNHasJudged.add(target)
        if (!replayModel.isScoreV2) {
            target.judgementStart = totalJudgement
        }
        return canBeJudgedAgain
    }
}