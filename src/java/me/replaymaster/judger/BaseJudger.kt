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
internal const val HIT_AND_CAN_HIT_AGAIN = 1
internal const val TOO_LATE = 2
internal const val IGNORE = 3

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

    open fun judgeRelease(action: Note, target: Note): Boolean {
        return false
    }

    open fun onFindTarget(action: Note, target: Note) {
        action.offSetStart = (action.timeStamp - target.timeStamp).toInt()
        action.offSetEnd = (action.endTime - target.endTime).toInt()
    }

    open fun getJudgement(diff: Double, note: Note? = null, action: Note? = null): Int {
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
            val canHit = canHit(action, note)
            when {
                canHit == IGNORE -> continue
                canHit == TOO_EARLY -> break
                canHit == TOO_LATE || cannotJudge.contains(note) -> shouldDelete = true
                else -> {
                    // hit!
                    if (candidate == null) {
                        candidate = note
                        if (canHit != HIT_AND_CAN_HIT_AGAIN) {
                            listNode.remove()
                        }
                        break
                    }
                }
            }
            if (shouldDelete) {
                listNode.remove()
            }
        }
        candidate?.let { notNullCandidate ->
            onFindTarget(action, notNullCandidate)
        }
        return candidate
    }

    override fun judge() {
        for (action in replayModel.replayData) {
            val target = findTarget(action)
            if (target == null) {
                action.showAsLN = false
                continue
            }
            val judgement = getJudgement(abs(action.timeStamp - target.timeStamp).toDouble(), target, action)
            action.judgementStart = judgement
            target.judgementStart = judgement
            if (target.duration == 0L) {
                action.showAsLN = false
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