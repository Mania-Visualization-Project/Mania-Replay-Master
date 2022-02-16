package me.replaymaster.model

data class ReplayModel(
        var replayData: List<Note>,
        val rate: Double,
        val mirror: Boolean,
        val judgement: DoubleArray
)