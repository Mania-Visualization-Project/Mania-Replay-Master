package me.replaymaster

import it.sauronsoftware.jave.DefaultFFMPEGLocator
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.lang.StringBuilder
import java.util.*
import kotlin.math.abs
import java.awt.SystemColor.info
import sun.reflect.annotation.AnnotationParser.toArray
import java.util.Arrays
import java.io.IOException
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import java.awt.SystemColor.info
import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.InputStream


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
    }

    @JvmStatic
    fun attachBgm(beatMap: BeatMap, videoFile: String, outFile: String) {
        val locator = DefaultFFMPEGLocator()
        val method = locator::class.java.getDeclaredMethod("getFFMPEGExecutablePath")
        method.isAccessible = true
        val ffmpegPath = method.invoke(locator) as String
        method.isAccessible = false

        println("FFmpeg path: $ffmpegPath")

        val ffmpeg = ProcessBuilder()
                .command(listOf(
                        ffmpegPath,
                        "-i", videoFile,
                        "-i", beatMap.bgmPath,
                        outFile
                ))
                .redirectErrorStream(true)
                .start()
        val stdout = BufferedReader(InputStreamReader(ffmpeg.getInputStream()))
        var line: String
        while (true) {
            line = stdout.readLine() ?: break
            print(line)
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