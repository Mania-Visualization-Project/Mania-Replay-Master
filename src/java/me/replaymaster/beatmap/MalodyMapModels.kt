package me.replaymaster.beatmap

import com.google.gson.annotations.SerializedName

class MCModel(
        @SerializedName("meta")
        val meta: MCMeta,
        @SerializedName("time")
        val time: List<MCTime>,
        @SerializedName("note")
        val note: List<MCNote>
)

class MCMeta(
        @SerializedName("mode_ext")
        val modeExt: MCModeExt
)

class MCModeExt(
        @SerializedName("column")
        val column: Int
)

abstract class MCBeatObject : Comparable<MCBeatObject> {

    abstract protected val _beat: List<Int>

    override fun compareTo(other: MCBeatObject): Int {
        return beatValue.compareTo(other.beatValue)
    }

    //    val beatValue: Double by lazy {
//        _beat[0] + _beat[1].toDouble() / _beat[2] + 1
//    }
    val beatValue: Double
        get() = _beat[0] + _beat[1].toDouble() / _beat[2] + 1
}

class MCTime(
        @SerializedName("beat")
        val beat: List<Int>,
        @SerializedName("bpm")
        val bpm: Double
) : MCBeatObject() {
    override val _beat: List<Int>
        get() = beat
}

class MCNote(
        @SerializedName("beat")
        val beat: List<Int>,
        @SerializedName("endbeat")
        val endBeat: List<Int>?,
        @SerializedName("column")
        val column: Int?,
        @SerializedName("sound")
        val sound: String?, // last item
        @SerializedName("offset")
        val offset: Double? // last item
) : MCBeatObject() {

    override val _beat: List<Int>
        get() = beat

    val endBeatValue: Double?
        get() = endBeat?.let {
            it[0] + it[1].toDouble() / it[2] + 1
        }
}