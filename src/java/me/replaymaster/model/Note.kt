package me.replaymaster.model

data class Note(
        var timeStamp: Long = 0,
        val column: Int = 0,
        var duration: Long = 0,
        var judgementStart: Int = -1, // miss
        var judgementEnd: Int = -1 // LN judgement
) : Comparable<Note> {

    val endTime: Long
        get() = duration + timeStamp

    override fun compareTo(other: Note): Int {
        return timeStamp.compareTo(other.timeStamp)
    }

    override fun toString(): String {
        return "$timeStamp $column $duration $judgementStart $judgementEnd\n"
    }

    fun scale(rate: Double) {
        duration = (duration / rate).toLong()
        timeStamp = (timeStamp / rate).toLong()
    }

    fun setJudgement(judgement: Int, start: Boolean) {
        if (start) {
            judgementStart = judgement
        } else {
            judgementEnd = judgement
        }
    }
}