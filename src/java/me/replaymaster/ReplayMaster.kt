package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.io.BufferedReader
import java.io.InputStreamReader
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
                    break
                }
            }

            if (targetNote != null) {

                unjudgeNotes.remove(targetNote)

                targetNote.judgement = -1
                action.judgement = -1
                for (i in 0..beatMap.judgementTime.lastIndex) {
                    if (minDiff <= beatMap.judgementTime[i]) {
                        targetNote.judgement = i
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
    }

    @JvmStatic
    fun attachBgm(beatMap: BeatMap, videoFile: String, outFile: String, ffmpegPath: String="ffmpeg") {
        println("FFmpeg path: $ffmpegPath")

        val ffmpeg = ProcessBuilder()
                .command(listOf(
                        ffmpegPath,
                        "-i", videoFile,
                        "-i", beatMap.bgmPath,
                        "-f", "avi",
                        "-y",
                        outFile
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
                              resultPath: String)
}