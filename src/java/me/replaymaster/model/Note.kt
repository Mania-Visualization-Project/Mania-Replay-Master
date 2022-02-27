package me.replaymaster.model

class Note(
        var timeStamp: Long = 0,
        var column: Int = 0,
        var duration: Long = 0,
        var offSetStart: Int = 0, // action, for monitor
        var offSetEnd: Int = 0, // action, for monitor
        var judgementStart: Int = -1, // miss
        var judgementEnd: Int = -1, // LN judgement
        var duelNote: Note? = null, // taiko big note, seperated into two duel notes
        var taikoIsRed: Boolean = true // taiko
) : Comparable<Note> {

    val endTime: Long
        get() = duration + timeStamp

    var showAsLN = true

    var relatedActionOrNote: Note? = null

    var index: Int = 0

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

    fun toSingleNote() = SingleNote(
            timeStamp, column, duration, offSetStart, offSetEnd, judgementStart, judgementEnd
    )

    class SingleNote(
            val timeStamp: Long = 0,
            val column: Int = 0,
            val duration: Long = 0,
            val offSetStart: Int = 0, // action, for monitor
            val offSetEnd: Int = 0, // action, for monitor
            val judgementStart: Int = -1, // miss
            val judgementEnd: Int = -1, // LN judgement
    )
}