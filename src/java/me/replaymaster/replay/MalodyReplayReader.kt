package me.replaymaster.replay

import me.replaymaster.*
import me.replaymaster.judger.MalodyJudger
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replays.Mods
import me.replays.stream.OsuBinaryInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

data class MalodyRawHitEvent(
        val timeStamp: Long,
        val pressOrRelease: Int,
        val column: Int
) : Comparable<MalodyRawHitEvent> {
    override fun compareTo(other: MalodyRawHitEvent): Int {
        val timeCompare = timeStamp.compareTo(other.timeStamp)
        if (timeCompare != 0) {
            return timeCompare
        }
        return -pressOrRelease.compareTo(other.pressOrRelease)
    }
}

object MalodyReplayReader : IReplayReader {

    override val extension = "mr"

    val JUDGEMENT_TABLE = arrayOf(
            doubleArrayOf(52.0, 92.0, 126.0, 150.0), // Judge A
            doubleArrayOf(44.0, 84.0, 118.0, 150.0), // Judge B
            doubleArrayOf(36.0, 76.0, 110.0, 150.0), // Judge C
            doubleArrayOf(28.0, 68.0, 102.0, 150.0), // Judge D
            doubleArrayOf(20.0, 60.0, 94.0, 150.0) // Judge E
    )

    override fun readBeatMapMD5(replayPath: String): String {
        OsuBinaryInputStream(FileInputStream(replayPath)).use {
            checkFormatValid(replayPath, it.readMalodyString() == "mr format head")
            readByteAsHexString(it, 4) // version
            return it.readMalodyString()
        }
    }

    private fun getJudgement(judgeLevel: Int, rate: Double): DoubleArray {
        return JUDGEMENT_TABLE[judgeLevel].map {
            (it + if (Config.INSTANCE.isMalodyPE) 9 else 0) / rate
        }.toDoubleArray()
    }

    override fun getJudger(beatMap: BeatMap, replayModel: ReplayModel) = MalodyJudger(beatMap, replayModel)

    override fun readReplay(path: String, beatMap: BeatMap): ReplayModel {
        OsuBinaryInputStream(FileInputStream(path)).use {
            checkFormatValid(path, it.readMalodyString() == "mr format head")
            val version = readByteAsHexString(it, 4)
            val md5 = it.readMalodyString()
            val difficulty = it.readMalodyString()
            val mapName = it.readMalodyString()
            val author = it.readMalodyString()
            val score = it.int32
            val combo = it.int32
            val best = it.int32
            val cool = it.int32
            val good = it.int32
            val miss = it.int32
            val miss2 = it.int32
            debug("Judgement results in replay: $best, $cool, $good, $miss")
            Main.judgementFromReplay = arrayListOf(best, cool, good, miss)
            val mod = it.int32
            debug("Mod: $mod, ${Integer.toBinaryString(mod)}, ${MalodyMod.toString(mod)}")
            val rate = when {
                MalodyMod.DASH.belongTo(mod) -> 1.2
                MalodyMod.RASH.belongTo(mod) -> 1.5
                MalodyMod.SLOW.belongTo(mod) -> 0.8
                else -> 1.0
            }
            logLine("parse.replay.rate", rate)
            val judge = it.int32 // judge, 0-4: A-E

            logLine("read.beatmap.malody.judgement", ('A' + judge).toString() + if (Config.INSTANCE.isMalodyPE) " (PE)" else " (PC)")
            val judgement = getJudgement(judge, rate)
            logLine("read.beatmap.judgement", Arrays.toString(judgement))

            checkFormatValid(path, it.readMalodyString() == "mr data")
            readByteAsHexString(it, 4) // version

            val eventCount = it.int32
            val unknown5 = readByteAsHexString(it, 1)
            val playTime = it.int32
            debug("Play time: ${Date(playTime * 1000L)}")
            val unknown6 = readByteAsHexString(it, 4)
            debug("Malody .mr extra: $unknown5, $unknown6")

            val currentHold = hashMapOf<Int, Note>()
            val notes = arrayListOf<Note>()

            val malodyRawEvents = arrayListOf<MalodyRawHitEvent>()
            for (i in 0 until eventCount) {
                val timeStamp = it.int32.toLong()
                val pressOrRelease = it.readByte().toInt() and 0xFF // 1: press; 2: release
                val column = it.readByte().toInt() and 0xFF
                val data = MalodyRawHitEvent(timeStamp, pressOrRelease, column)
                malodyRawEvents.add(data)
            }

            malodyRawEvents.sort()
            for (i in 0..malodyRawEvents.lastIndex) {
                val malodyRawEvent = malodyRawEvents[i]
                val timeStamp = malodyRawEvent.timeStamp
                val pressOrRelease = malodyRawEvent.pressOrRelease
                val column = malodyRawEvent.column

                if (timeStamp < 0) {
                    continue
                }
                checkFormatValid(path, pressOrRelease == 2 || pressOrRelease == 1)
                checkFormatValid(path, column < beatMap.key)
                if (pressOrRelease == 1) {
                    // press
                    if (currentHold.containsKey(column)) {
                        currentHold[column]!!.duration = timeStamp - currentHold[column]!!.timeStamp
                        currentHold.remove(column)
                    }
                    val newNote = Note(timeStamp, column, 10000)
                    currentHold[column] = newNote
                    notes.add(newNote)
                } else {
                    // release
                    if (currentHold.containsKey(column)) {
                        currentHold[column]!!.duration = timeStamp - currentHold[column]!!.timeStamp
                        currentHold.remove(column)
                    }
                }
            }

            return ReplayModel(notes, rate, MalodyMod.FLIP.belongTo(mod), judgement).apply {
                adjust(beatMap, this)
            }
        }
    }

    private fun readByteAsHexString(inputStream: InputStream, count: Int): String {
        val array = ByteArray(count)
        inputStream.read(array)
        return array.map {
            val str = Integer.toHexString(it.toInt() and 0xFF).toUpperCase()
            (if (str.length == 1) "0" else "") + str
        }
                .joinToString()

    }

    private fun checkFormatValid(path: String, valid: Boolean) {
        if (!valid) {
            throw InvalidReplayException(path, Exception())
        }
    }
}

fun OsuBinaryInputStream.readMalodyString(): String {
    val count = int32
    val bytes = ByteArray(count)
    this.read(bytes)
    return String(bytes)
}

enum class MalodyMod(
        val flag: Int
) {
    NO_MOD(0),
    LUCK(1 shl 1),
    FLIP(1 shl 2),
    CONST(1 shl 3),
    DASH(1 shl 4),
    RASH(1 shl 5),
    HIDE(1 shl 6),

    SLOW(1 shl 8),
    DEATH(1 shl 9);

    companion object {
        fun toString(value: Int): String {
            val result = arrayListOf<MalodyMod>()
            for (m in values()) {
                if (m.belongTo(value)) {
                    result.add(m)
                }
            }
            return result.map { it.name }.joinToString()
        }
    }

    fun belongTo(value: Int) = (value and flag) > Mods.None.value()

}