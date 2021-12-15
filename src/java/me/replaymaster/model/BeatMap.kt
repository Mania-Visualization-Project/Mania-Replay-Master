package me.replaymaster.model

import me.replaymaster.logLine
import java.io.File

data class BeatMap(
        var key: Int = -1,
        var notes: List<Note> = emptyList(),
        var bgmPath: String = MUTE_MUSIC,
        var bgmOffset: Int = 0,
        var od: Double = 0.0,  // osu!
        var type: Int = TYPE_MANIA,
        var sliderMultiplier: Double? = null // osu!taiko
) {

    companion object {
        private val MUSIC_EXTENSION = listOf("mp3", "ogg", "wav")
        const val MUTE_MUSIC = "mute_sound.mp3"

        const val TYPE_MANIA = 0
        const val TYPE_TAIKO = 1
    }

    fun checkValidation() {
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

    var duration = 0L

    val isMania
        get() = type == TYPE_MANIA

    val isTaiko
        get() = type == TYPE_TAIKO
}
