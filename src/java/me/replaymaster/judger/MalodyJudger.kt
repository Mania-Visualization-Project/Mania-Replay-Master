package me.replaymaster.judger

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel

class MalodyJudger(
        beatMap: BeatMap,
        replayModel: ReplayModel
) : BaseJudger(beatMap, replayModel) {
    override fun canHit(action: Note, note: Note): Int {
        if (action.column != note.column) {
            return IGNORE
        }
        val diff = action.timeStamp - note.timeStamp
        return if (diff < -judgementWindow.last()) {
            TOO_EARLY
        } else if (diff > judgementWindow.last()) {
            TOO_LATE
        } else {
            HIT
        }
    }

    // Malody has no tail judgement!
    override fun judgeRelease(action: Note, target: Note): Boolean {
        target.judgementEnd = target.judgementStart
        action.judgementEnd = action.judgementStart
        return false
    }
}