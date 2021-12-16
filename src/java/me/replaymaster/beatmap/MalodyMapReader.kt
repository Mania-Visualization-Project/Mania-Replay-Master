package me.replaymaster.beatmap

import com.google.gson.Gson
import me.replaymaster.logLine
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Note
import java.io.File

private class MalodyBPMStamp(val time: Double, val bpm: Double, val beatValue: Double)

object MalodyMapReader : IMapReader {

    override val mapExtension: String = "mc"
    override val zipExtension: String = "mcz"

    override fun readMap(path: String): BeatMap {
        val malodyMapModel = Gson().fromJson(File(path).readText(), MCModel::class.java)

        // meta info
        val metaNote = malodyMapModel.note.last { it.sound != null }
        val bgmName = checkNotNull(metaNote.sound) { "Invalid .mc file: sound not found!" }
        val offset = metaNote.offset ?: 0.0
        val key = malodyMapModel.meta.modeExt.column

        // time
        val bpmList = arrayListOf<MalodyBPMStamp>()
        val timeList = malodyMapModel.time.sorted()
        for (i in 0..timeList.lastIndex) {
            val timeObject = timeList[i]
            val curBeatValue = timeObject.beatValue
            val curBpm = timeObject.bpm
            if (i == 0) {
                bpmList.add(MalodyBPMStamp(0.0, curBpm, curBeatValue))
            } else {
                val curTime = beatToTime(curBeatValue, bpmList.last())
                bpmList.add(MalodyBPMStamp(curTime, curBpm, curBeatValue))
            }
        }

        // note
        val noteList = malodyMapModel.note.sorted()
                .filter { it.column != null }
                .map {
                    val startTime = beatToTimeWithBPM(it.beatValue, bpmList)
                    val endTime = beatToTimeWithBPM(it.endBeatValue
                            ?: it.beatValue, bpmList)
                    val column = it.column!!
                    Note(startTime.toLong(), column, endTime.toLong() - startTime.toLong())
                }
        logLine("read.beatmap.mode", "malody:key")

        return BeatMap(key, noteList, File(path).parentFile.resolve(bgmName).absolutePath, bgmOffset = offset.toInt(),
                gameMode = "malody:key")
    }

    private fun beatToTime(beatValue: Double, lastStamp: MalodyBPMStamp): Double {
        return (beatValue - lastStamp.beatValue) * 60000 / lastStamp.bpm + lastStamp.time
    }

    private fun beatToTimeWithBPM(beatValue: Double, bpmList: List<MalodyBPMStamp>): Double {
        var position = bpmList.indexOfFirst { it.beatValue > beatValue }
        if (position == -1) {
            position = bpmList.size
        }
        position -= 1
        return beatToTime(beatValue, bpmList[position])
    }
}