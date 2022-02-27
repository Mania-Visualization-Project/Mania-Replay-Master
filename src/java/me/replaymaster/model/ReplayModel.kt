package me.replaymaster.model

data class ReplayModel(
        var replayData: List<Note>,
        val rate: Double,
        val mirror: Boolean,
        val judgement: DoubleArray,
        val isScoreV2: Boolean = false // osu!mania
) {
    var acc: Double = 0.0
}