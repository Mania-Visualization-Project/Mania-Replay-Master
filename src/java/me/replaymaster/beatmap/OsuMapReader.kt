package me.replaymaster.beatmap

import me.replaymaster.ModeNotSupportedException
import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.floor

object OsuMapReader : IMapReader {

    override val mapExtension: String = "osu"
    override val zipExtension: String = "osz"

    private const val STATUS_UNKNOWN = 0
    private const val STATUS_TIME = 1
    private const val STATUS_OBJECTS = 2

    private fun parseAttributes(key: String, value: String, beatMap: BeatMap, beatMapFile: File) {
        when (key) {
            "OverallDifficulty" -> {
                beatMap.od = value.toDouble()
                logLine("read.beatmap.od", beatMap.od)
            }

            "AudioFilename" -> {
                beatMap.bgmPath = File(beatMapFile.parent, value.trim()).absolutePath
                logLine("read.beatmap.bgm", beatMap.bgmPath)
            }

            "Mode" -> {
                when (value) {
                    "0" -> {
                        beatMap.type = BeatMap.TYPE_OSU_STD
                        beatMap.gameMode = "osu!std"
                    }
                    "1" -> {
                        beatMap.type = BeatMap.TYPE_TAIKO
                        beatMap.gameMode = "osu!taiko"
                        beatMap.key = 4
                    }
                    "3" -> {
                        beatMap.type = BeatMap.TYPE_MANIA
                        beatMap.gameMode = "osu!mania"
                    }
                    else -> {
                        throw ModeNotSupportedException(value)
                    }
                }
                logLine("read.beatmap.mode", beatMap.gameMode)
            }

            "SliderMultiplier" -> {
                beatMap.sliderMultiplier = value.toDoubleOrNull()
            }

            "CircleSize" -> {
                if (beatMap.isMania) {
                    beatMap.key = value.toInt()
                    logLine("read.beatmap.key", beatMap.key)
                }
            }
        }
    }

    private fun parseHitObjects(line: String, notes: MutableList<Note>, beatMap: BeatMap) {
        val elements = line.split(",")
        val timeStamp = elements[2].toLong()
        val type = elements[3].toInt()

        when (beatMap.type) {

            BeatMap.TYPE_MANIA -> {
                val column = floor(elements[0].toInt() * beatMap.key / 512.0).toInt()
                val endTime = elements[5].split(":")[0].toLong()
                val duration = if (type and 128 != 0) endTime - timeStamp else 0L
                val note = Note(timeStamp, column, duration)
                notes.add(note)
            }

            BeatMap.TYPE_TAIKO -> {
                if (type and 1 != 0) { // hit circle
                    val hitSound = elements[4].toInt()
                    val isRim = (hitSound and 2 != 0) || (hitSound and 8 != 0)
                    val note = if (isRim) { // rim
                        Note(timeStamp, 0)
                    } else {
                        Note(timeStamp, 1)
                    }
                    notes.add(note)
                    if ((hitSound and 4) != 0) { // Big note
                        val duelNote = Note(timeStamp, 3 - note.column, duelNote = note)
                        note.duelNote = duelNote
                        notes.add(duelNote)
                    }
                } else if (type and (1 shl 1) != 0) { // drum rolls
                    // ignore temporarily
                } else if (type and (1 shl 3) != 0) { // denden notes
                    // ignore temporarily
                }
            }
        }

    }

    override fun readMap(path: String): BeatMap {
        val inputFile = File(path)
        val inputStream: InputStream = inputFile.inputStream()
        val beatMap = BeatMap()
        val notes = arrayListOf<Note>()
        beatMap.notes = notes
        var status = STATUS_UNKNOWN
        inputStream.bufferedReader().lines().forEach {
            val line = it.trim()
            when {
                line == "[TimingPoints]" -> {
                    status = STATUS_TIME
                }
                line == "[HitObjects]" -> {
                    status = STATUS_OBJECTS
                    logLine("read.beatmap.notes")
                }
                line.startsWith("[") -> {
                    status = STATUS_UNKNOWN
                }
                else -> {
                    when (status) {
                        STATUS_UNKNOWN -> {
                            if (line.contains(":")) {
                                val lines = line.split(":", limit = 2)
                                parseAttributes(lines[0].trim(), lines[1].trim(), beatMap, inputFile)
                            }
                        }

                        STATUS_TIME -> {

                        }

                        STATUS_OBJECTS -> {
                            parseHitObjects(line, notes, beatMap)
                        }

                    }
                }
            }
        }
        inputStream.close()
        return beatMap
    }
}