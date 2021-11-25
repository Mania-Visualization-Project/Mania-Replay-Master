package me.replaymaster.judger

import me.replaymaster.Main
import me.replaymaster.debug
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.abs

internal const val TOO_EARLY = -1
internal const val HIT = 0
internal const val TOO_LATE = 1

interface IJudger {
    fun judge()
}

abstract class BaseJudger(
        val beatMap: BeatMap,
        val replayModel: ReplayModel
) : IJudger {
    private var notesToJudge = LinkedList(beatMap.notes)
    private val cannotJudge = HashSet<Note>()
    internal val judgementWindow = replayModel.judgement

    abstract fun canHit(action: Note, note: Note): Int

    abstract fun judgeRelease(action: Note, target: Note): Boolean

    internal fun getJudgement(diff: Double): Int {
        for (i in 0 until (judgementWindow.size - 1)) {
            if (diff <= judgementWindow[i]) {
                return i
            }
        }
        return -1
    }

    private fun findTarget(action: Note): Note? {
        val listNode = notesToJudge.iterator()
        var candidate: Note? = null
        while (listNode.hasNext()) {
            val note = listNode.next()
            var shouldDelete = false
            if (note.column == action.column) {
                val canHit = canHit(action, note)
                if (canHit == TOO_EARLY) break
                if (canHit == TOO_LATE || cannotJudge.contains(note)) {
                    shouldDelete = true
                } else { // hit!
                    if (candidate == null) {
                        candidate = note
                        if (note.duration == 0L) {
                            cannotJudge.add(note)
                        }
                    }
                }
                if (shouldDelete) {
                    listNode.remove()
                }
            }
        }
        candidate?.let { notNullCandidate ->
            action.offSetStart = (action.timeStamp - notNullCandidate.timeStamp).toInt()
            action.offSetEnd = (action.endTime - notNullCandidate.endTime).toInt()
        }
        return candidate
    }

    override fun judge() {
        for (action in replayModel.replayData) {
            val target = findTarget(action)
            if (target == null) {
                action.duration = 0
                continue
            }
            val judgement = getJudgement(abs(action.timeStamp - target.timeStamp).toDouble())
            action.judgementStart = judgement
            target.judgementStart = judgement
            if (target.duration == 0L) {
                action.duration = 0
            } else {
                val canBeJudgedAgain = judgeRelease(action, target)
                if (!canBeJudgedAgain) {
                    cannotJudge.add(target)
                }
            }
        }

        // for debug
        val count = hashMapOf<Int, Int>()
        for (note in beatMap.notes) {
            count[note.judgementStart] = count.getOrDefault(note.judgementStart, 0) + 1
        }
        val judgementResult = count.entries
                .sortedBy { if (it.key == -1) Int.MAX_VALUE else it.key }
                .map { it.value }.toList()
        Main.judgementFromJudging = judgementResult
        debug("Judgement results: $judgementResult")
    }

}