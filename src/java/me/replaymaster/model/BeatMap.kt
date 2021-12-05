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

    companion object {
        private val MUSIC_EXTENSION = listOf("mp3", "ogg", "wav")
        const val MUTE_MUSIC = "mute_sound.mp3"
    }

    init {
        val bgmFile = File(bgmPath)
        if (!bgmFile.exists()) {
            val parent = bgmFile.parentFile
            val path = parent.listFiles()?.firstOrNull {
                it.extension == bgmFile.extension || it.extension in MUSIC_EXTENSION
            }?.absolutePath ?: MUTE_MUSIC
            logLine("warning.bgm_not_found", bgmPath, path)
            bgmPath = path
        }
    }

    val duration: Long
        get() = notes.last().endTime + 2000
}
