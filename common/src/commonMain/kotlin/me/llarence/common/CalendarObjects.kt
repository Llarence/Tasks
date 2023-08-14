package me.llarence.common

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds

// This code is kinda dumb, but it is all hidden in this file
// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage
// TODO: Look into making it more optimized
// TODO: Move some of the math into functions

// Task and Event are not in the json
abstract class CalendarObject() {
    // Kinda dumb but I want the scope mostly for .toPx()
    // It feels like less should be passed into these functions
    abstract val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Float) -> Float?
    abstract val preDrawFun: DrawScope.(Instant, Float, Float, Float) -> Unit
    abstract val drawFun: DrawScope.(Instant, Boolean, Float, Float, Float) -> Unit
    abstract val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Float) -> Unit

    abstract fun toJson(): JSONObject

    fun inBounds(pointerInputScope: PointerInputScope, time: Instant, offset: Offset, textBuffer: Float, daySize: Float, scroll: Float): Float? {
        return inBoundsFun(pointerInputScope, time, offset, textBuffer, daySize, scroll)
    }

    fun preDraw(drawScope: DrawScope, time: Instant, textBuffer: Float, daySize: Float, scroll: Float) {
        preDrawFun(drawScope, time, textBuffer, daySize, scroll)
    }

    fun draw(drawScope: DrawScope, time: Instant, grabbed: Boolean, textBuffer: Float, daySize: Float, scroll: Float) {
        drawFun(drawScope, time, grabbed, textBuffer, daySize, scroll)
    }

    fun drag(pointerInputScope: PointerInputScope, time: Instant, change: Offset, grabbedOffset: Float, textBuffer: Float, daySize: Float, scroll: Float) {
        dragFun(pointerInputScope, time, change, grabbedOffset, textBuffer, daySize, scroll)
    }
}

abstract class ColorableCalendarObject(var color: Color) : CalendarObject() {
    override fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("color", color.value.toLong())

        return json
    }
}

class CalendarEvent(val event: Event, color: Color) : ColorableCalendarObject(color) {
    constructor(event: Event, json: JSONObject) : this(event, Color(json.getLong("color").toULong()))

    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Float) -> Float? = { time, offset, textBuffer, daySize, scroll ->
        val diff = event.time - time
        if (diff < 7.days) {
            val x = textBuffer + ((daySize + DAY_PADDING) * diff.inWholeDays)
            val relaX = x - offset.x
            if (0 >= relaX && relaX + daySize > 0) {
                val dayDiff = diff - diff.inWholeDays.days
                val y = (dayDiff.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() + scroll
                val relaY = y - offset.y
                if (0 >= relaY && relaY + (event.duration.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() > 0) {
                    relaY
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    override val preDrawFun: DrawScope.(Instant, Float, Float, Float) -> Unit = { _, _, _, _ ->
    }

    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Float) -> Unit = { time, grabbed, textBuffer, daySize, scroll ->
        val diff = event.time - time
        if (diff < 7.days) {
            val currColor = if (grabbed) {
                color * DARKEN_PERCENT
            } else {
                color
            }

            val x = textBuffer + ((daySize + DAY_PADDING) * diff.inWholeDays)
            val dayDiff = diff - diff.inWholeDays.days
            val y = (dayDiff.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() + scroll
            val height = (event.duration.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx()

            drawRoundRect(currColor, Offset(x, y), Size(daySize, height), CornerRadius(CORNER_RADIUS.dp.toPx()))
        }
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Float) -> Unit = { time, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS_IN_WEEK - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), HOURS_IN_DAY - (event.duration.inWholeNanoseconds * HOURS_IN_NANO))

        event.time = time + day.days + hour.hours
    }
}

class CalendarTask(val task: Task, inColor: Color) : ColorableCalendarObject(inColor) {
    constructor(task: Task, json: JSONObject) : this(task, Color(json.getLong("color").toULong()))

    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Float) -> Float? = { time, offset, textBuffer, daySize, scroll ->
        val diff = task.dueTime - time
        if (diff < 7.days) {
            val x = textBuffer + ((daySize + DAY_PADDING) * diff.inWholeDays)
            val relaX = x - offset.x
            if (0 >= relaX && relaX + daySize > 0) {
                val dayDiff = diff - diff.inWholeDays.days
                val y = (dayDiff.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() + scroll
                val relaY = y - offset.y
                if (0 >= relaY && relaY + (TASK_HOURS * HOUR_SIZE).dp.toPx() > 0) {
                    relaY
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            null
        }
    }

    val pathTo: DrawScope.(Float, Float, Long, Float, Float, Float, Float, PathEffect) -> Unit = { xFrom, yFrom, days, hours, textBuffer, daySize, scroll, pathEffect ->
        val xTo = textBuffer + ((daySize + DAY_PADDING) * days) + daySize / 2f
        val yTo = (hours * HOUR_SIZE).dp.toPx() + scroll

        val path = Path()
        path.moveTo(xFrom, yFrom)
        path.cubicTo(xTo, yFrom + PATH_STRENGTH.dp.toPx(), xFrom, yTo - PATH_STRENGTH.dp.toPx(), xTo, yTo)
        drawPath(path, color, style = Stroke(pathEffect = pathEffect))
    }

    override val preDrawFun: DrawScope.(Instant, Float, Float, Float) -> Unit = { time, textBuffer, daySize, scroll ->
        val diff = task.dueTime - time
        if (diff < 7.days) {
            val xFrom = textBuffer + ((daySize + DAY_PADDING) * diff.inWholeDays) + daySize / 2f
            val dayDiff = diff - diff.inWholeDays.days
            val yFrom = (dayDiff.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() + scroll + (TASK_HOURS * HOUR_SIZE).dp.toPx()

            val interval = PATH_INTERVALS.dp.toPx()
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval))
            if (task.event != null) {
                val eventDiff = task.event!!.time - time
                if (eventDiff < 7.days) {
                    val eventDayDiff = eventDiff - eventDiff.inWholeDays.days
                    pathTo(this, xFrom, yFrom, eventDiff.inWholeDays, eventDayDiff.inWholeNanoseconds * HOURS_IN_NANO, textBuffer, daySize, scroll, pathEffect)
                }
            }

            for (requiredTask in task.requirements) {
                val taskDiff = task.event!!.time - time
                if (taskDiff < 7.days) {
                    val taskDayDiff = taskDiff - taskDiff.inWholeDays.days
                    pathTo(this, xFrom, yFrom, taskDiff.inWholeDays, taskDayDiff.inWholeNanoseconds * HOURS_IN_NANO, textBuffer, daySize, scroll, pathEffect)
                }
            }
        }
    }

    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Float) -> Unit = { time, grabbed, textBuffer, daySize, scroll ->
        val diff = task.dueTime - time
        if (diff < 7.days) {
            val currColor = if (grabbed) {
                color * DARKEN_PERCENT
            } else {
                color
            }

            val x = textBuffer + ((daySize + DAY_PADDING) * diff.inWholeDays)
            val dayDiff = diff - diff.inWholeDays.days
            val y = (dayDiff.inWholeNanoseconds * HOURS_IN_NANO * HOUR_SIZE).dp.toPx() + scroll
            val height = (TASK_HOURS * HOUR_SIZE).dp.toPx()

            val triangle = Path()
            triangle.moveTo(x, y)
            triangle.relativeLineTo(daySize, 0f)
            triangle.relativeLineTo(-daySize / 2f, height)
            drawPath(triangle, currColor)
        }
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Float) -> Unit = { time, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS_IN_WEEK - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), HOURS_IN_DAY - TASK_HOURS)

        task.dueTime = time + day.days + hour.hours
    }
}

class CalendarDummy : CalendarObject() {
    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Float) -> Float? = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override val preDrawFun: DrawScope.(Instant, Float, Float, Float) -> Unit = { _, _, _, _ ->
        throw NotImplementedError()
    }

    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Float) -> Unit = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Float) -> Unit = { _, _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override fun toJson(): JSONObject {
        throw NotImplementedError()
    }
}
