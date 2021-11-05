package me.replaymaster.model

data class BeatMap(
        val key: Int,
        val notes: List<Note>,
        val bgmPath: String,
        val bgmOffset: Int=0,
        val od: Double = 0.0
) {
    val duration: Long
        get() = notes.last().endTime + 2000
}
