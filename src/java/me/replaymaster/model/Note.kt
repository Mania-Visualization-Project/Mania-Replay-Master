package me.replaymaster.model

import java.io.OutputStream
import java.nio.ByteBuffer

data class Note(
        val timeStamp: Long = 0,
        val column: Int = 0,
        val duration: Long = 0,
        var judgement: Int = -1 // miss
): Comparable<Note> {
    override fun compareTo(other: Note): Int {
        return timeStamp.compareTo(other.timeStamp)
    }

    override fun toString(): String {
        return "$timeStamp $column $duration $judgement\n"
    }
}