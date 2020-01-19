package me.replaymaster.model

data class Note(
        var timeStamp: Long = 0,
        val column: Int = 0,
        var duration: Long = 0,
        var judgement: Int = -1 // miss
): Comparable<Note> {
    override fun compareTo(other: Note): Int {
        return timeStamp.compareTo(other.timeStamp)
    }

    override fun toString(): String {
        return "$timeStamp $column $duration $judgement\n"
    }

    fun scale(rate: Double) {
        duration = (duration / rate).toLong()
        timeStamp = (timeStamp / rate).toLong()
    }
}