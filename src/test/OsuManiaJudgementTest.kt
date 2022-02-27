import me.replaymaster.beatmap.OsuMapReader
import me.replaymaster.judger.OsuManiaJudger
import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import me.replaymaster.replay.OsuReplayReader
import java.io.File
import java.text.SimpleDateFormat
import kotlin.test.assertEquals

class OsuManiaJudgementTest {

    @org.junit.Test
    fun testRice() {
        val judgementWindow = doubleArrayOf(16.0, 34.0, 67.0, 97.0, 121.0, 158.0)
        val beatMap = BeatMap(1, listOf(Note(1000)))

        for (i in judgementWindow.indices) {
            val startTime = (1000 - judgementWindow[i] + 1).toLong()
            val replay = ReplayModel(listOf(Note(startTime)), 1.0, false, judgementWindow)
            OsuManiaJudger(beatMap, replay).judge()
            assertEquals(beatMap.notes[0].judgementStart, if (i == judgementWindow.lastIndex) -1 else i)
        }

    }

    @org.junit.Test
    fun testLN() {
        val judgementWindow = doubleArrayOf(16.0, 34.0, 67.0, 97.0, 121.0, 158.0)
        val beatMap = BeatMap(1, listOf(
                Note(1000, duration = 150),
                Note(1200, duration = 150),
                Note(1400, duration = 150),
        ))
        val replay = ReplayModel(listOf(
                Note(1000, duration = 10),
                Note(1200, duration = 150),
                Note(1400, duration = 150),
        ), 1.0, false, judgementWindow, true)

        OsuManiaJudger(beatMap, replay).judge()
        print(beatMap.notes)
        print(replay.replayData)
    }

    @org.junit.Test
    fun testLNMapV1() {
        Config.INSTANCE.debug = true
        val beatMap = OsuMapReader.readMap("test\\Sound Horizon - Raijin no Hidariude (-Kamikaze-) [Tempest w _underjoy].osu")
        val replay = OsuReplayReader.readReplay("test\\Kuiiiiteeee - Sound Horizon - Raijin no Hidariude [Tempest w _underjoy] (2018-09-23) OsuMania.osr", beatMap)
        OsuReplayReader.getJudger(beatMap, replay).judge()
    }

    @org.junit.Test
    fun testLNMapV2() {
        Config.INSTANCE.debug = true
        Config.INSTANCE.omScoreV2Adjust = false
//        val beatMap = OsuMapReader.readMap("test\\ck - Carnation (ck remix) (Gekido-) [LN Master].osu")
//        val replay = OsuReplayReader.readReplay("test\\- ck - Carnation (ck remix) [LN Master] (2021-12-06) OsuMania.osr", beatMap)
        val beatMap = OsuMapReader.readMap("test\\Various Artists - 4K LN Dan Courses v2 - Extra Level - (_underjoy) [12th Dan - Yuugure (Marathon)].osu")
        beatMap.notes = beatMap.notes.filter { it.timeStamp >= 294000 && it.timeStamp <= 420000 }
        val replay = OsuReplayReader.readReplay("test\\Kuit - Various Artists - 4K LN Dan Courses v2 - Extra Level - [12th Dan - Yuugure (Marathon)] (2022-01-08) OsuMania.osr", beatMap)
        val judger = OsuReplayReader.getJudger(beatMap, replay)
        judger.judge()

        fun parseTime(timestamp: Long): String {
            var t = timestamp
            val h = (timestamp / 1000 / 3600).toInt()
            t -= h * 3600 * 1000

            val m = (t / 1000 / 60).toInt()
            t -= m * 60 * 1000

            val s = (t / 1000).toInt()
            t -= s * 1000
            return String.format("%02d:%02d:%02d,%03d", h, m, s, t)
        }

        var lastTime = 0L
        var n = 1
        val content = StringBuilder()
        content.appendLine(n).appendLine("${parseTime(lastTime)} --> ${parseTime(beatMap.notes.first().timeStamp + 2000)}")
                .appendLine("<font size=60>${String.format("%.2f", 100.0)}</font>")
                .appendLine()
        lastTime = beatMap.notes.first().timeStamp

        for (i in beatMap.notes.indices) {
            if (beatMap.notes[i].timeStamp - lastTime > 20) {
                val acc = judger.calculateAccuracy(beatMap.notes.subList(0, i + 1))

                content.appendLine(n).appendLine("${parseTime(lastTime)} --> ${parseTime(beatMap.notes[i].timeStamp)}")
                        .appendLine("<font size=60>${String.format("%.2f", acc * 100)}</font>")
                        .appendLine()
                n += 1
                lastTime = beatMap.notes[i].timeStamp
            }
        }

        content.appendLine(n).appendLine("${parseTime(lastTime)} --> ${parseTime(beatMap.notes.last().endTime + 2000)}")
                .appendLine("<font size=60>${String.format("%.2f", judger.calculateAccuracy(beatMap.notes) * 100)}</font>")
                .appendLine()

    }

    @org.junit.Test
    fun testRiceMapV1() {
        Config.INSTANCE.debug = true
        val beatMap = OsuMapReader.readMap("test\\Doin - Pine nut (Night Bunny 7) [ATTang's Lv.27].osu")
        val replay = OsuReplayReader.readReplay("test\\Kuit - Doin - Pine nut [ATTang's Lv.27] (2020-10-03) OsuMania.osr", beatMap)
        OsuReplayReader.getJudger(beatMap, replay).judge()
    }
}