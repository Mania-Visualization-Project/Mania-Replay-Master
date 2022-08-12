package me.replaymaster

import me.replaymaster.model.BeatMap
import me.replaymaster.model.Config
import me.replaymaster.model.Note
import me.replaymaster.model.ReplayModel
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
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

    private val gameHeight = Config.INSTANCE.height
    private val gameWidth = if (beatMap.isTaiko && Config.INSTANCE.taikoSingleColumn) {
        Config.INSTANCE.width / 4
    } else {
        Config.INSTANCE.width
    }
    private val columnWidth = Config.INSTANCE.width / beatMap.key
    private val totalHeight = ensureMultiple(timeToHeight(beatMap.duration.toDouble()), Config.INSTANCE.speed)
    private val stepSize = ensureMultiple(Config.INSTANCE.maxStepSize.toDouble(), pixelPerFrame)
    private val windowSize = stepSize + gameHeight

    private val bufferedImage = BufferedImage(gameWidth, windowSize, BufferedImage.TYPE_INT_ARGB)
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

    private fun rectangle(p1: Pair<Int, Int>, p2: Pair<Int, Int>, color: Color, strokeWidth: Int, filledColor: Color? = null) {
        val w = abs(p2.first - p1.first)
        val h = abs(p2.second - p1.second)
        val x = min(p1.first, p2.first)
        val y = min(p1.second, p2.second)
        graphics2D.run {
            paint = color
            if (strokeWidth != -1) {
                if (filledColor != null) {
                    paint = filledColor
                    fillRoundRect(x, y, w, h, 1, 1)
                    paint = color
                }
                stroke = BasicStroke(strokeWidth.toFloat() * 2 - 1)
                drawRoundRect(x, y, w, h, 1, 1)

            } else {
                fillRoundRect(x, y, w, h, 1, 1)
            }
        }
    }

    private fun drawRound(p: Pair<Int, Int>, r: Int, color: Color, strokeWidth: Int, filledColor: Color? = null) {
        graphics2D.run {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            paint = color

            if (strokeWidth != -1) {
                if (filledColor != null) {
                    paint = filledColor
                    fillOval(p.first - r, p.second - r, 2 * r, 2 * r)
                    paint = color
                }
                stroke = BasicStroke(strokeWidth.toFloat() * 2 - 1)
                drawOval(p.first - r, p.second - r, 2 * r, 2 * r)
            } else {
                fillOval(p.first - r, p.second - r, 2 * r, 2 * r)
            }
        }
    }

    private fun renderTaikoNote(note: Note, time: Double) {
        val x = ((note.column + 0.5) * columnWidth).toInt()
        val y = timeToHeight(time).toInt()
        val r = (columnWidth * 0.4).toInt()
        drawRound(x to y, r, judgementToColor(note.judgementStart),
                Config.INSTANCE.stroke, )
//        rectangle((x - r) to y, (x + r) to y, judgementToColor(note.judgementStart),
//                Config.INSTANCE.stroke)
        drawRound(x to y, Config.INSTANCE.stroke, judgementToColor(note.judgementStart), Config.INSTANCE.stroke)
    }

    private fun renderNote(note: Note, time: Double, isBase: Boolean, judgement: Int, debugText: String? = null) {
        if (isBase && beatMap.isTaiko) {
            renderTaikoNote(note, time)
            return
        }
        var x = (note.column * columnWidth).toDouble()
        val y = timeToHeight(time)
        var h = max(Config.INSTANCE.blockHeight.toDouble(), timeToHeight(note.duration.toDouble()))
        var width = columnWidth.toDouble()
        if (!isBase) {
            h = Config.INSTANCE.actionHeight.toDouble()
            x += width / 5
            width -= 2 * width / 5
        }

        var filledColor: Color? = null
        if (beatMap.isTaiko && Config.INSTANCE.taikoSingleColumn) {
            filledColor = if (note.taikoIsRed) {
                readColor(Config.INSTANCE.taikoForegroundRed)
            } else {
                readColor(Config.INSTANCE.taikoForegroundBlue)
            }
        }
        if (y - h < 0) h = y
        val color = judgementToColor(judgement)
        x += columnWidth * 0.1
        width -= 2 * columnWidth * 0.1
        val stroke = if (isBase) {
            Config.INSTANCE.stroke
        } else -1
        rectangle(x.toInt() to y.toInt(),
                (x + width).toInt() to (y - h).toInt(),
                color, stroke, filledColor)

        if (debugText != null) {
            graphics2D.run {
                paint = color
                drawString(debugText, (x + Config.INSTANCE.stroke).toInt(), (y - Config.INSTANCE.stroke).toInt())
            }
        }
    }

    private fun judgementToColor(judgement: Int): Color {
        val colorText = if (judgement != -1) {
            Config.INSTANCE.judgementColor[judgement]
        } else {
            Config.INSTANCE.missColor
        }
        return readColor(colorText)
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
        val y = timeToHeight(time) // - Config.INSTANCE.blockHeight.toDouble() / 2
        val width: Int = Config.INSTANCE.actionHeight / 2
        rectangle(
                start.toInt() to (y - width).toInt(),
                end.toInt() to (y + width).toInt(),
                readColor(Config.INSTANCE.longNoteColor),
                -1
        )
    }

    private fun renderActionConnectionLine(time: Double, note: Note, action: Note) {
        val actionHeight = Config.INSTANCE.actionHeight
        val width: Int = actionHeight / 2
        val x = (note.column * columnWidth) + columnWidth / 2
        val y1 = timeToHeight(time - note.timeStamp).toInt()
        val y2 = timeToHeight(time - action.timeStamp).toInt()
        val y = max(y1, y2)
        var h = y - min(y1, y2)
        if (y < h) h = y
        rectangle((x - width) to y1, (x + width) to y2,
                judgementToColor(action.judgementStart),
                -1)
    }

    private fun render(data: List<Note>, time: Double, isBase: Boolean) {
        if (beatMap.isTaiko && isBase && !Config.INSTANCE.taikoSingleColumn) {
            // taiko big note: draw a connection line
            for (note in data) {
                if (note.duelNote != null && note.column <= 1) {
                    renderConnectionLine(time - note.timeStamp, note, note.duelNote!!)
                }
            }
        }

        for (i in data.indices) {
            val note = data[i]
            var debugText: String? = null
            if (isBase && Config.INSTANCE.showTimestamp) {
                debugText = ((note.timeStamp - 1000) * replayModel.rate).toString()
            }
            renderNote(note, time - note.timeStamp, isBase, note.judgementStart, debugText)
            if (!isBase && (note.showAsLN || Config.INSTANCE.showHolding)) { // hold LN, render end
                renderNote(note, time - note.endTime, false,
                        note.judgementEnd)
                renderActionLN(note, time)
            }
        }

        if (beatMap.isTaiko && isBase && Config.INSTANCE.taikoSingleColumn) {
            data
                .filter { it.relatedActionOrNote != null && it.relatedActionOrNote!!.judgementStart != -1 }
                .forEach {
//                    renderActionConnectionLine(time, it, it.relatedActionOrNote!!)
                }
        }
    }

    fun start(): Pair<Int, Int> {

        var i = 0
        var currentStart = 0
        val windowCount = ceil((totalHeight - gameHeight).toDouble() / stepSize).toInt()

        while (currentStart < totalHeight) {

            clear()
            if (beatMap.isTaiko) {
                if (!Config.INSTANCE.taikoSingleColumn) {
                    // taiko: multi column
                    // draw background
                    graphics2D.run {
                        val w = bufferedImage.width / beatMap.key
                        val padding = bufferedImage.width - w * 4
                        paint = readColor(Config.INSTANCE.taikoBackgroundBlue)
                        fillRect(padding, 0, w, bufferedImage.height)
                        fillRect(padding + w * 3, 0, w, bufferedImage.height)
                        paint = readColor(Config.INSTANCE.taikoBackgroundRed)
                        fillRect(padding + w, 0, w * 2, bufferedImage.height)
                    }
                }
            }

            render(beatMap.notes, heightToTime(currentStart + windowSize), true)
            render(replayModel.replayData, heightToTime(currentStart + windowSize), false)

            ImageIO.write(bufferedImage, "PNG",
                    File(outputImage.path.replace(".png", "_$i.png")))
            currentStart += stepSize
            i += 1
            ReplayMaster.printProgress("render", 0, 30, (i.toDouble() * 100 / windowCount))
        }
        val windowFrameCount = stepSize / pixelPerFrame
        return windowFrameCount to gameWidth
    }

    private fun readColor(colorHex: String): Color {
        return when (colorHex.length) {
            6 -> {
                Color(Integer.parseInt(colorHex, 16))
            }
            8 -> {
                Color(Integer.parseUnsignedInt(colorHex, 16), true)
            }
            else -> {
                error("Unknown colorHex: $colorHex")
            }
        }
    }
}