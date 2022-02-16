package me.replaymaster.judger

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import kotlin.math.abs

class OsuTaikoJudger(
        beatMap: BeatMap,
        replayModel: ReplayModel
) : BaseJudger(beatMap, replayModel) {

    companion object {
        private const val J_300 = 0
        private const val J_100 = 1
        private const val J_MISS = 2
    }

    private val firstHitNoteToAction = hashMapOf<Note, Note>()
    private val wrongHitNoteToAction = hashMapOf<Note, Note>()

    override fun canHit(action: Note, note: Note): Int {
        val diff = action.timeStamp - note.timeStamp
        if (isWrongAction(action, note)) {
            if (note.duelNote != null && note.duelNote!! in firstHitNoteToAction) {
                // second hit: ignore wrong action
                return IGNORE
            }
            return if (abs(diff) <= judgementWindow[J_MISS]) {
                wrongHitNoteToAction[note] = action
                HIT_AND_CAN_HIT_AGAIN
            } else {
                IGNORE
            }
        }

        // correct action
        if (diff < -judgementWindow[J_MISS]) {
            return TOO_EARLY
        } else if (diff > judgementWindow[J_MISS]) {
            return TOO_LATE
        }

        // in the judgable interval
        if (note in wrongHitNoteToAction) {
            val previousWrongAction = wrongHitNoteToAction[note]!!
            wrongHitNoteToAction.remove(note)
            if (previousWrongAction.timeStamp != action.timeStamp) {
                return IGNORE
            }
        }

        if (note.duelNote == null) {
            // normal note
            return HIT
        }

        // big note
        val duelNote = note.duelNote!!
        if (duelNote !in firstHitNoteToAction) {
            // first hit big note
            firstHitNoteToAction[note] = action
            return HIT
        }

        // second hit big note
        val firstAction = firstHitNoteToAction[duelNote]!!
        return when {
            action.timeStamp > firstAction.endTime -> {
                // first action has released: too late
                TOO_LATE
            }
            abs(action.timeStamp - firstAction.timeStamp) < 30 -> {
                // second action time - first action time < 30: can hit
                HIT
            }
            else -> {
                // second action time - first action time >= 30: too late
                TOO_LATE
            }
        }
    }

    override fun onFindTarget(action: Note, target: Note) {
        super.onFindTarget(action, target)
        if (target.column + action.column == 3) {
            target.column = action.column
            target.duelNote?.column = 3 - action.column
        }
    }

    override fun getJudgement(diff: Double, note: Note?, action: Note?): Int {
        checkNotNull(note)
        checkNotNull(action)
        if (isWrongAction(note, action)) {
            return -1
        }
        if (diff >= judgementWindow[J_100]) {
            return -1
        }
        // big note: use the previous judgement
        if (note.duelNote != null && note.duelNote!!.judgementStart <= J_100 && note.duelNote!!.judgementStart >= J_300) {
            return note.duelNote!!.judgementStart
        }
        if (diff < judgementWindow[J_300]) {
            return J_300
        }
        if (diff < judgementWindow[J_100]) {
            return J_100
        }
        return -1
    }

    private fun isWrongAction(action: Note, note: Note): Boolean {
        return note.column != action.column && note.column + action.column != 3
    }

    override fun judge() {
        super.judge()

        if (Config.INSTANCE.taikoSingleColumn) {
            // taiko: single column
            beatMap.notes = beatMap.notes.filter {
                it.duelNote == null || it.column <= 1
            }
                    .map { note ->
                        note.column = 0
                        if (note.duelNote != null && note.judgementStart == -1) {
                            note.judgementStart = note.duelNote!!.judgementStart
                        }
                        note
                    }
            replayModel.replayData = replayModel.replayData.filter {
                val relatedNote = it.relatedActionOrNote ?: return@filter true
                relatedNote.judgementStart != -1
            }
                    .map { note ->
                        note.column = 0
                        note
                    }
        }


//        val realNotes = beatMap.notes
//        val combineNotes = arrayListOf<Note>()
//        realNotes.forEach { note ->
//            note.column += 1
//            if (note.duelNote != null && note.column <= 2) {
//                return@forEach
//            }
//            val judgement = if (note.duelNote != null && note.judgementStart == -1) {
//                note.duelNote!!.judgementStart
//            } else {
//                note.judgementStart
//            }
//            combineNotes.add(
//                    Note(note.timeStamp, judgementStart = judgement).apply {
//                        taikoInitialColumn = note.column - 1
//                    }
//            )
//        }
//        beatMap.notes = (realNotes + combineNotes).sorted()
//        beatMap.key += 1
//
//        replayModel.replayData.forEach {
//            it.column += 1
//        }
//        replayModel.replayData = (replayModel.replayData + replayModel.replayData.map { note ->
//            Note(timeStamp = note.timeStamp, judgementStart = note.judgementStart).apply {
//                showAsLN = note.showAsLN
//            }
//        }).sorted()
    }
}