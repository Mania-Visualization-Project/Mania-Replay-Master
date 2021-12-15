package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

class Render(
        private val beatMap: BeatMap,
        private val replayModel: ReplayModel,
        private val outputImage: File
) {

    private val pixelPerFrame = Config.INSTANCE.speed
    private val timeHeightRatio = pixelPerFrame / 1000.0 * Config.INSTANCE.framePerSecond
    private fun timeToHeight(t: Double) = t * timeHeightRatio
    private fun heightToTime(h: Int) = h / timeHeightRatio

    private val columnWidth = Config.INSTANCE.width / beatMap.key
    private val h = Config.INSTANCE.height
    private val H = ensureMultiple(timeToHeight(beatMap.duration.toDouble()), Config.INSTANCE.speed)
    private val stepSize = ensureMultiple(Config.INSTANCE.maxStepSize.toDouble(), pixelPerFrame)
    private val N = ceil((H - h).toDouble() / stepSize).toInt()
    private val windowSize = stepSize + h

    private val bufferedImage = BufferedImage(Config.INSTANCE.width, windowSize, BufferedImage.TYPE_INT_ARGB)
    private var graphics2D = bufferedImage.createGraphics()

    private fun ensureMultiple(x: Double, base: Int): Int {
        return (ceil(x / base).toInt() * base).apply {
            debug("ensureMultiple: $x -> $this")
        }
    }

    private fun clear() {
        graphics2D.run {
            paint = Color.BLACK
            fillRect(0, 0, bufferedImage.width, bufferedImage.height)
        }
        graphics2D.dispose()
        graphics2D = bufferedImage.createGraphics()
    }

    private fun rectangle(p1: Pair<Int, Int>, p2: Pair<Int, Int>, color: Color, strokeWidth: Int) {
        val w = abs(p2.first - p1.first)
        val h = abs(p2.second - p1.second)
        val x = min(p1.first, p2.first)
        val y = min(p1.second, p2.second)
        graphics2D.run {
            paint = color
            if (strokeWidth != -1) {
                stroke = BasicStroke(strokeWidth.toFloat() * 2 - 1)
                drawRoundRect(x, y, w, h, 1, 1)
            } else {
                fillRoundRect(x, y, w, h, 1, 1)
            }
        }
    }

    private fun renderNote(note: Note, time: Double, isBase: Boolean, judgement: Int) {
        var x = (note.column * columnWidth).toDouble()
        val y = timeToHeight(time)
        var h = max(Config.INSTANCE.blockHeight.toDouble(), timeToHeight(note.duration.toDouble()))
        var width = columnWidth.toDouble()
        if (!isBase) {
            h = Config.INSTANCE.actionHeight.toDouble()
            x += width / 5
            width -= 2 * width / 5
        }
        if (y - h < 0) h = y
        val colorText = if (judgement != -1) {
            Config.INSTANCE.judgementColor[judgement]
        } else {
            Config.INSTANCE.missColor
        }
        val color = readColor(colorText)
        x += columnWidth * 0.1
        width -= 2 * columnWidth * 0.1
        rectangle(x.toInt() to y.toInt(),
                (x + width).toInt() to (y - h).toInt(),
                color, if (isBase) Config.INSTANCE.stroke else -1)
    }

    private fun renderActionLN(note: Note, currentTime: Double) {
        val actionHeight = Config.INSTANCE.actionHeight
        val width: Int = actionHeight / 2
        val x = (note.column * columnWidth) + columnWidth / 2
        val y = timeToHeight(currentTime - note.timeStamp).toInt()
        var h = timeToHeight(note.duration.toDouble()).toInt()
        if (y < h) h = y
        var currentH: Int = actionHeight * 2
        while (currentH + actionHeight <= h) {
            rectangle((x - width) to (y - currentH),
                    (x + width) to (y - currentH - actionHeight),
                    readColor(Config.INSTANCE.longNoteColor),
                    -1)
            currentH += actionHeight * 2
        }
    }

    private fun renderConnectionLine(time: Double, leftNote: Note, rightNote: Note) {
        val start = (leftNote.column + 1) * columnWidth - columnWidth * 0.1
        val end = rightNote.column * columnWidth + columnWidth * 0.1
        val y = timeToHeight(time) - Config.INSTANCE.blockHeight.toDouble() / 2
        val width: Int = Config.INSTANCE.actionHeight / 2
        rectangle(
                start.toInt() to (y - width).toInt(),
                end.toInt() to (y + width).toInt(),
                readColor(Config.INSTANCE.longNoteColor),
                -1
        )
    }

    private fun render(data: List<Note>, time: Double, isBase: Boolean) {
        for (note in data) {
            if (isBase && note.duelNote != null && note.column <= 1) {
                // taiko big note: draw a connection line
                renderConnectionLine(time - note.timeStamp, note, note.duelNote!!)
            }
        }
        for (note in data) {
            renderNote(note, time - note.timeStamp, isBase, note.judgementStart)
            if (!isBase && note.showAsLN) { // hold LN, render end
                renderNote(note, time - note.endTime, false,
                        note.judgementEnd)
                renderActionLN(note, time)
            }
        }
    }

    fun start(): Int {

        var i = 0
        var currentStart = 0
        while (currentStart < H) {

            clear()
            if (beatMap.isTaiko) {
                graphics2D.run {
                    val w = bufferedImage.width / 4
                    paint = readColor(Config.INSTANCE.taikoBackgroundBlue)
                    fillRect(0, 0, w, bufferedImage.height)
                    fillRect(w * 3, 0, w, bufferedImage.height)
                    paint = readColor(Config.INSTANCE.taikoBackgroundRed)
                    fillRect(w, 0, w * 2, bufferedImage.height)
                }
            }

            render(beatMap.notes, heightToTime(currentStart + windowSize), true)
            render(replayModel.replayData, heightToTime(currentStart + windowSize), false)

            ImageIO.write(bufferedImage, "PNG",
                    File(outputImage.path.replace(".png", "_$i.png")))
            currentStart += stepSize
            i += 1
            ReplayMaster.printProgress("render", 0, 30, (i.toDouble() * 100 / N))
        }
        val windowFrameCount = stepSize / pixelPerFrame
        return windowFrameCount
    }

    private fun readColor(colorHex: String): Color {
        return when (colorHex.length) {
            6 -> {
                Color(Integer.parseInt(colorHex, 16))
            }
            8 -> {
                Color(Integer.parseInt(colorHex, 16), true)
            }
            else -> {
                error("Unknown colorHex: $colorHex")
            }
        }
    }
}