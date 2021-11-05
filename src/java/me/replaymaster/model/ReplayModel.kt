package me.replaymaster.model

data class ReplayModel(
        val replayData: List<Note>,
        val rate: Double,
        val mirror: Boolean,
        val judgement: DoubleArray
)