package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


object ReplayMaster {

    val STAGE_RENDER = 30
    val STAGE_SPLICT = 60

    private fun getJudgement(diff: Double, judgementTime: DoubleArray): Int {
        for (i in 0..judgementTime.lastIndex) {
            if (diff <= judgementTime[i]) {
                return if (i == judgementTime.lastIndex) -1 else i
            }
        }
        return -1
    }

    private fun judgeNote(base: Note, action: Note, judgementTime: DoubleArray, start: Boolean = true) {
        base.setJudgement(-1, start)
        action.setJudgement(-1, start)
        val diff = if (start) {
            abs(base.timeStamp - action.timeStamp)
        } else {
            abs(base.endTime - action.endTime)
        }

        for (i in 0..judgementTime.lastIndex) {
            if (diff <= judgementTime[i]) {
                base.setJudgement(i, start)
                action.setJudgement(i, start)
                break
            }
        }
    }

    @JvmStatic
    fun judge(beatMap: BeatMap, replay: ReplayModel, judgeLn: Boolean) {
        val unjudgeNotes = LinkedList<Note>(beatMap.notes)
        for (action in replay.replayData) {

            var targetNote: Note? = null
            val oldDuration = action.duration
            action.duration = 0 // mark rice

            for (note in unjudgeNotes) {
                if (note.column != action.column) continue

                val diff = note.timeStamp - action.timeStamp
                if (abs(diff) > replay.judgement.last()) {
                    if (diff > 0) break
                    continue
                }
                targetNote = note
                break
            }

            if (targetNote != null) {

                unjudgeNotes.remove(targetNote)

                val holdDiff = abs(targetNote.timeStamp - action.timeStamp)
                val holdJudgement = getJudgement(holdDiff.toDouble(), replay.judgement)
                targetNote.setJudgement(holdJudgement, true)
                action.setJudgement(holdJudgement, true)

                if (targetNote.duration > 0) { // LN
                    action.duration = oldDuration
                    action.setJudgement(holdJudgement, false)
                    if (judgeLn) {
                        val releaseDiff = abs(targetNote.endTime - action.endTime) / 1.5
                        val releaseJudgement = getJudgement(releaseDiff, replay.judgement)
                        action.setJudgement(releaseJudgement, false)

                        // judgement of long note is the average of hold judgement and release judgement (including lenience)
                        val lnJudgement = getJudgement((releaseDiff + holdDiff) / 2.0, replay.judgement)
                        targetNote.setJudgement(lnJudgement, true)
                    }
                }
            }
        }

        // for debug
        val count = hashMapOf<Int, Int>()
        for (note in beatMap.notes) {
            count[note.judgementStart] = count.getOrDefault(note.judgementStart, 0) + 1
        }
        val judgementResult = count.entries.sortedBy { it.key }.map { it.value }.toList()
        Main.judgementFromJudging = judgementResult
        debug("Judgement results: $count")
    }

    @JvmStatic
    fun generateVideo(beatMap: BeatMap, videoFile: File, outFile: File, rate: Double, delay: Int,
                      windowFrameCount: Int, ffmpegPath: String = "ffmpeg") {
        Encoder(beatMap, videoFile, outFile, rate, delay, windowFrameCount, ffmpegPath).generateVideo()
    }

    private val startTimes = hashMapOf<String, Long>()
    private var preProgress = 0.0

    fun printProgress(stage: String, startProgress: Int, endProgress: Int, interProgress: Double) {
        var progress = max(0.0, min(interProgress, 100.0)) / 100 * (endProgress - startProgress) + startProgress
        progress = max(preProgress, progress)
        preProgress = progress
        if (stage !in startTimes.keys) {
            startTimes[stage] = System.currentTimeMillis()
        }
        if (interProgress >= 100) {
            debug("$stage: ${(System.currentTimeMillis() - startTimes[stage]!!) / 1000.0} s")
        }

        if (Config.INSTANCE.isServer) {
            File(Config.INSTANCE.outputDir).resolve("progress.txt").writeText(progress.toString())
        }

        // UI
        val progressSize = 50
        fun getNChar(count: Int, ch: String) = Collections.nCopies(max(count, 0), ch).joinToString("")
        val solidCount = (progress / 100.0 * progressSize).toInt()
        val voidCount = progressSize - solidCount
        val progressText = String.format("%03.2f%%├", progress)
        val totalCount = solidCount + voidCount + progressText.length + 1

        print(getNChar(totalCount, "\b"))
        print(progressText)

        print(getNChar(solidCount, "█"))
        print(getNChar(voidCount, "-"))
        print("┤")
    }
}