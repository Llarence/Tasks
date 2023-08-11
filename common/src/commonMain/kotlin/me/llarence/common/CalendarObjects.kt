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
import org.json.JSONObject
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun sameWeek(date1: Calendar, date2: Calendar): Boolean {
    return date1.get(Calendar.WEEK_OF_YEAR) == date2.get(Calendar.WEEK_OF_YEAR) && date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
}

// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage

// TODO: Only render on the week that the calendarObject is on
// TODO: Move some of the math into functions
// Task and Event are not in the json
abstract class CalendarObject {
    // Kinda dumb but I want the scope mostly for .toPx()
    // It feels like less should be passed into these functions
    abstract val inBoundsFun: PointerInputScope.(Calendar, Offset, Float, Float, Float) -> Float?
    abstract val preDrawFun: DrawScope.(Calendar, Float, Float, Float) -> Unit
    abstract val drawFun: DrawScope.(Calendar, Boolean, Float, Float, Float) -> Unit
    abstract val dragFun: PointerInputScope.(Calendar, Offset, Float, Float, Float, Float) -> Unit

    abstract fun toJson(): JSONObject

    fun inBounds(pointerInputScope: PointerInputScope, calendarDate: Calendar, offset: Offset, textBuffer: Float, daySize: Float, scroll: Float): Float? {
        return inBoundsFun(pointerInputScope, calendarDate, offset, textBuffer, daySize, scroll)
    }

    fun preDraw(drawScope: DrawScope, calendarDate: Calendar, textBuffer: Float, daySize: Float, scroll: Float) {
        preDrawFun(drawScope, calendarDate, textBuffer, daySize, scroll)
    }

    fun draw(drawScope: DrawScope, calendarDate: Calendar, grabbed: Boolean, textBuffer: Float, daySize: Float, scroll: Float) {
        drawFun(drawScope, calendarDate, grabbed, textBuffer, daySize, scroll)
    }

    fun drag(pointerInputScope: PointerInputScope, calendarDate: Calendar, change: Offset, grabbedOffset: Float, textBuffer: Float, daySize: Float, scroll: Float) {
        dragFun(pointerInputScope, calendarDate, change, grabbedOffset, textBuffer, daySize, scroll)
    }
}

abstract class ColorableCalendarObject(var color: Color) : CalendarObject() {
    override fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("color", color.value.toLong())

        return json
    }
}

class CalendarEvent(val event: Event, inColor: Color) : ColorableCalendarObject(inColor) {
    constructor(event: Event, json: JSONObject) : this(event, Color(json.getLong("color").toULong()))

    override val inBoundsFun: PointerInputScope.(Calendar, Offset, Float, Float, Float) -> Float? = { calendarDate, offset, textBuffer, daySize, scroll ->
        if (sameWeek(event.time.date, calendarDate)) {
            val x = textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1))
            val relaX = x - offset.x
            if (0 >= relaX && relaX + daySize > 0) {
                val y = (event.time.hour * HOUR_SIZE).dp.toPx() + scroll
                val relaY = y - offset.y
                if (0 >= relaY && relaY + (event.duration * HOUR_SIZE).dp.toPx() > 0) {
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

    override val preDrawFun: DrawScope.(Calendar, Float, Float, Float) -> Unit = { _, _, _, _ ->
    }

    override val drawFun: DrawScope.(Calendar, Boolean, Float, Float, Float) -> Unit = { calendarDate, grabbed, textBuffer, daySize, scroll ->
        if (sameWeek(event.time.date, calendarDate)) {
            val currColor = if (grabbed) {
                color * DARKEN_PERCENT
            } else {
                color
            }

            val x = textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1))
            val y = (HOUR_SIZE * event.time.hour).dp.toPx() + scroll
            val height = (event.duration * HOUR_SIZE).dp.toPx()

            drawRoundRect(currColor, Offset(x, y), Size(daySize, height), CornerRadius(CORNER_RADIUS.dp.toPx()))
        }
    }

    override val dragFun: PointerInputScope.(Calendar, Offset, Float, Float, Float, Float) -> Unit = { calendarDate, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), 24f - event.duration)

        event.time.date.set(Calendar.DAY_OF_WEEK, day + 1)
        event.time.hour = hour
    }
}

class CalendarTask(val task: Task, inColor: Color) : ColorableCalendarObject(inColor) {
    constructor(task: Task, json: JSONObject) : this(task, Color(json.getLong("color").toULong()))

    override val inBoundsFun: PointerInputScope.(Calendar, Offset, Float, Float, Float) -> Float? = { calendarDate, offset, textBuffer, daySize, scroll ->
        if (sameWeek(task.dueTime.date, calendarDate)) {
            val x = textBuffer + ((daySize + DAY_PADDING) * (task.dueTime.date.get(Calendar.DAY_OF_WEEK) - 1))
            val relaX = x - offset.x
            if (0 >= relaX && relaX + daySize > 0) {
                val y = (task.dueTime.hour * HOUR_SIZE).dp.toPx() + scroll
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

    val pathTo: DrawScope.(Float, Float, Time, Float, Float, Float, PathEffect) -> Unit = { xFrom, yFrom, time, textBuffer, daySize, scroll, pathEffect ->
        val xTo = textBuffer + ((daySize + DAY_PADDING) * (time.date.get(Calendar.DAY_OF_WEEK) - 1)) + daySize / 2f
        val yTo = (HOUR_SIZE * time.hour).dp.toPx() + scroll

        val path = Path()
        path.moveTo(xFrom, yFrom)
        path.cubicTo(xTo, yFrom + PATH_STRENGTH.dp.toPx(), xFrom, yTo - PATH_STRENGTH.dp.toPx(), xTo, yTo)
        drawPath(path, color, style = Stroke(pathEffect = pathEffect))
    }

    override val preDrawFun: DrawScope.(Calendar, Float, Float, Float) -> Unit = { calendarDate, textBuffer, daySize, scroll ->
        if (sameWeek(task.dueTime.date, calendarDate)) {
            val xFrom = textBuffer + ((daySize + DAY_PADDING) * (task.dueTime.date.get(Calendar.DAY_OF_WEEK) - 1)) + daySize / 2f
            val yFrom = (HOUR_SIZE * task.dueTime.hour).dp.toPx() + scroll + (TASK_HOURS * HOUR_SIZE).dp.toPx()

            val interval = PATH_INTERVALS.dp.toPx()
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval))
            if (task.event != null && sameWeek(task.event!!.time.date, calendarDate)) {
                pathTo(this, xFrom, yFrom, task.event!!.time, textBuffer, daySize, scroll, pathEffect)
            }

            for (requiredTask in task.requirements) {
                if (sameWeek(requiredTask.dueTime.date, calendarDate)) {
                    pathTo(this, xFrom, yFrom, requiredTask.dueTime, textBuffer, daySize, scroll, pathEffect)
                }
            }
        }
    }

    override val drawFun: DrawScope.(Calendar, Boolean, Float, Float, Float) -> Unit = { calendarDate, grabbed, textBuffer, daySize, scroll ->
        if (sameWeek(task.dueTime.date, calendarDate)) {
            val currColor = if (grabbed) {
                color * DARKEN_PERCENT
            } else {
                color
            }

            val x = textBuffer + ((daySize + DAY_PADDING) * (task.dueTime.date.get(Calendar.DAY_OF_WEEK) - 1))
            val y = (HOUR_SIZE * task.dueTime.hour).dp.toPx() + scroll
            val height = (TASK_HOURS * HOUR_SIZE).dp.toPx()

            val triangle = Path()
            triangle.moveTo(x, y)
            triangle.relativeLineTo(daySize, 0f)
            triangle.relativeLineTo(-daySize / 2f, height)
            drawPath(triangle, currColor)
        }
    }

    override val dragFun: PointerInputScope.(Calendar, Offset, Float, Float, Float, Float) -> Unit = { calendarDate, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), 24f - TASK_HOURS)

        task.dueTime.date.set(Calendar.DAY_OF_WEEK, day + 1)
        task.dueTime.hour = hour
    }
}

class CalendarDummy : CalendarObject() {
    override val inBoundsFun: PointerInputScope.(Calendar, Offset, Float, Float, Float) -> Float? = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override val preDrawFun: DrawScope.(Calendar, Float, Float, Float) -> Unit = { _, _, _, _ ->
        throw NotImplementedError()
    }

    override val drawFun: DrawScope.(Calendar, Boolean, Float, Float, Float) -> Unit = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override val dragFun: PointerInputScope.(Calendar, Offset, Float, Float, Float, Float) -> Unit = { _, _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override fun toJson(): JSONObject {
        throw NotImplementedError()
    }
}
