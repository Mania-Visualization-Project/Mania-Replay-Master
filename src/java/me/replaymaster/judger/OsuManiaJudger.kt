package me.replaymaster.judger

import me.replaymaster.model.BeatMap
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
        val isTooLate: Boolean
        if (note.duration != 0L) {
            diff = action.timeStamp - note.endTime
            isTooLate = if (LNHasJudged.contains(note)) {
                diff > -judgementWindow[J_50]
            } else {
                diff >= judgementWindow[J_100]
            }
        } else {
            isTooLate = diff >= judgementWindow[J_100]
        }
        return if (isTooLate)
            TOO_LATE
        else if (note.duration == 0L)
            HIT
        else
            HIT_AND_CAN_HIT_AGAIN
    }

    private fun isLNJudgedWith(startDiff: Long, totalDiff: Long, judgement: Int, rate: Double): Boolean {
        val judgementTime = judgementWindow[judgement] * rate
        return startDiff <= judgementTime && totalDiff <= judgementTime * 2
    }

    override fun judgeRelease(action: Note, target: Note): Boolean {
        val endDiff = abs(action.endTime - target.endTime)
        action.judgementEnd = getJudgement(endDiff / 1.5)

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
        if (target.endTime - action.endTime > judgementWindow[J_50]) {
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
        target.judgementStart = totalJudgement
        return canBeJudgedAgain
    }
}