package me.replaymaster.model

data class BeatMap(
        val key: Int,
        val judgementTime: DoubleArray,
        val notes: List<Note>,
        val bgmPath: String,
        val releaseLenience: Double
)
