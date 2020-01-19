package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.lang.StringBuilder
import java.util.*
import kotlin.math.abs

object ReplayMaster {

    init {
        System.loadLibrary("librender")
    }

    @JvmStatic
    fun judge(beatMap: BeatMap, replay: List<Note>) {
        val unjudgeNotes = LinkedList<Note>(beatMap.notes)
        for (action in replay) {

            var targetNote: Note? = null
            var minDiff = Long.MAX_VALUE

            for (note in unjudgeNotes) {
                if (note.column != action.column) continue

                val diff = note.timeStamp - action.timeStamp
                if (diff > beatMap.judgementTime.last()) {
                    break
                }
                if (abs(diff) < minDiff) {
                    minDiff = abs(diff)
                    targetNote = note
                } else {
                    unjudgeNotes.remove(targetNote)
                    break
                }
            }

            if (targetNote != null) {
                for (i in 0..beatMap.judgementTime.lastIndex) {
                    if (minDiff <= beatMap.judgementTime[i]) {
                        action.judgement = i
                        break
                    }
                }

//                println(action.judgement)
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

    // TODO: options
    @JvmStatic
    fun render(beatMap: BeatMap, replay: List<Note>, outPath: String) {
        val beatString = writeNotes(beatMap.notes)
        val replayString = writeNotes(replay)
        nativeRender(beatMap.key, beatMap.notes.size, beatString, replay.size, replayString, outPath)
//        Runtime.getRuntime().exec("G:\\code\\java\\ManiaReplayMaster\\cpp\\cmake-build-debug\\render.exe $beat $replayName")
    }

    @JvmStatic
    fun renderCallback(progress: Float) {
        println(progress)
    }

    @JvmStatic
    external fun nativeRender(key: Int,
                              beatSize: Int, beatNotes: String,
                              replaySize: Int, replayNotes: String,
                              resultPath: String)
}