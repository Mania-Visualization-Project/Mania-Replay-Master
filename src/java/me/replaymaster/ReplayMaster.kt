package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.math.abs


object ReplayMaster {

    init {
        System.loadLibrary("librender")
    }

    private fun getJudgement(diff: Double, judgementTime: DoubleArray): Int {
        for (i in 0..judgementTime.lastIndex) {
            if (diff <= judgementTime[i]) {
                return i
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
                if (abs(diff) > beatMap.judgementTime.last()) {
                    if (diff > 0) break
                    continue
                }
                targetNote = note
                break
            }

            if (targetNote != null) {

                unjudgeNotes.remove(targetNote)

                val holdDiff = abs(targetNote.timeStamp - action.timeStamp)
                val holdJudgement = getJudgement(holdDiff.toDouble(), beatMap.judgementTime)
                targetNote.setJudgement(holdJudgement, true)
                action.setJudgement(holdJudgement, true)

                if (targetNote.duration > 0) { // LN
                    action.duration = oldDuration
                    if (judgeLn) {
                        val releaseDiff = abs(targetNote.endTime - action.endTime) / beatMap.releaseLenience
                        val releaseJudgement = getJudgement(releaseDiff, beatMap.judgementTime)
                        action.setJudgement(releaseJudgement, false)

                        // judgement of long note is the average of hold judgement and release judgement (including lenience)
                        val lnJudgement = getJudgement((releaseDiff + holdDiff) / 2.0, beatMap.judgementTime)
                        targetNote.setJudgement(lnJudgement, true)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun writeNotes(notes: List<Note>): String {
        val sb = StringBuilder()
        for (note in notes) {
            sb.append(note.toString())
        }
        return sb.toString()
    }

    @JvmStatic
    fun render(beatMap: BeatMap, replay: ReplayModel, outPath: String, speed: Int) {
        val beatString = writeNotes(beatMap.notes)
        val replayString = writeNotes(replay.replayData)
        nativeRender(beatMap.key, beatMap.notes.size, beatString, replay.replayData.size, replayString, outPath, speed)
    }

    @JvmStatic
    fun attachBgm(beatMap: BeatMap, videoFile: File, outFile: File, rate: Double, ffmpegPath: String = "ffmpeg") {

        val ffmpeg = ProcessBuilder()
                .command(listOf(
                        ffmpegPath,
                        "-i", videoFile.absolutePath,
                        "-i", beatMap.bgmPath,
                        "-filter:a", "atempo=$rate",
                        "-c:v", "copy",
                        "-y",
                        outFile.absolutePath
                ))
                .redirectErrorStream(true)
                .start()
        val stdout = BufferedReader(InputStreamReader(ffmpeg.getInputStream()))
        var line: String
        while (true) {
            line = stdout.readLine() ?: break
            println(line)
        }
        ffmpeg.waitFor()
        stdout.close()
    }

    @JvmStatic
    fun renderCallback(progress: Float) {
        println(progress)
    }

    @JvmStatic
    external fun nativeRender(key: Int,
                              beatSize: Int, beatNotes: String,
                              replaySize: Int, replayNotes: String,
                              resultPath: String, speed: Int)
}