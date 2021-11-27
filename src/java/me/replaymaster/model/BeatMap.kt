package me.replaymaster.model

import me.replaymaster.logLine
import java.io.File

data class BeatMap(
        val key: Int,
        val notes: List<Note>,
        var bgmPath: String,
        val bgmOffset: Int=0,
        val od: Double = 0.0
) {

    init {
        val bgmFile = File(bgmPath)
        if (!bgmFile.exists()) {
            val path = bgmFile.parentFile.listFiles()?.firstOrNull { it.extension == bgmFile.extension }
            if (path != null) {
                logLine("warning.bgm_not_found", bgmPath, path)
                bgmPath = path.absolutePath
            }
        }
    }
    val duration: Long
        get() = notes.last().endTime + 2000
}
