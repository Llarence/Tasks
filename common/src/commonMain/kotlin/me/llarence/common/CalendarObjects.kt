package me.llarence.common

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage

// TODO: Only render on the week that the calendarObject is on
abstract class CalendarObject {
    // Kinda dumb but I want the scope mostly for .toPx()
    // It feels like less should be passed into these functions
    abstract val inBoundsFun: PointerInputScope.(Offset, Float, Float, Float) -> Boolean
    abstract val drawFun: DrawScope.(Boolean, Float, Float, Float) -> Unit
    abstract val dragFun: PointerInputScope.(Offset, Float, Float, Float, Float) -> Unit

    fun inBounds(pointerInputScope: PointerInputScope, offset: Offset, textBuffer: Float, daySize: Float, scroll: Float): Boolean {
        return inBoundsFun(pointerInputScope, offset, textBuffer, daySize, scroll)
    }

    fun draw(drawScope: DrawScope, grabbed: Boolean, textBuffer: Float, daySize: Float, scroll: Float) {
        drawFun(drawScope, grabbed, textBuffer, daySize, scroll)
    }

    fun drag(pointerInputScope: PointerInputScope, change: Offset, grabbedOffset: Float, textBuffer: Float, daySize: Float, scroll: Float) {
        dragFun(pointerInputScope, change, grabbedOffset, textBuffer, daySize, scroll)
    }
}

abstract class ColorableCalendarObject(var color: Color) : CalendarObject() {

}

class CalendarEvent(val event: Event, color: Color) : ColorableCalendarObject(color) {
    override val inBoundsFun: PointerInputScope.(Offset, Float, Float, Float) -> Boolean = { offset, textBuffer, daySize, scroll ->
        val x = textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1))
        val relaX = x - offset.x
        if (0 >= relaX && relaX + daySize > 0) {
            val y = (event.time.hour * HOUR_SIZE).dp.toPx() + scroll
            val relaY = y - offset.y
            if (0 >= relaY && relaY + (event.duration * HOUR_SIZE).dp.toPx() > 0) {
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override val drawFun: DrawScope.(Boolean, Float, Float, Float) -> Unit = { grabbed, textBuffer, daySize, scroll ->
        val color = if (grabbed) {
            color * DARKEN_PERCENT
        } else {
            color
        }

        val x = textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1))
        val y = (HOUR_SIZE * event.time.hour).dp.toPx() + scroll
        val height = (event.duration * HOUR_SIZE).dp.toPx()

        drawRoundRect(color, Offset(x, y), Size(daySize, height), CornerRadius(CORNER_RADIUS.dp.toPx()))
    }

    override val dragFun: PointerInputScope.(Offset, Float, Float, Float, Float) -> Unit = { change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), 24f - event.duration)

        event.time.date.set(Calendar.DAY_OF_WEEK, day + 1)
        event.time.hour = hour
    }
}

class CalendarTask(val task: Task, color: Color) : ColorableCalendarObject(color) {
    override val inBoundsFun: PointerInputScope.(Offset, Float, Float, Float) -> Boolean = { offset, textBuffer, daySize, scroll ->
        val x = textBuffer + ((daySize + DAY_PADDING) * (task.dueTime.date.get(Calendar.DAY_OF_WEEK) - 1))
        val relaX = x - offset.x
        if (0 >= relaX && relaX + daySize > 0) {
            val y = (task.dueTime.hour * HOUR_SIZE).dp.toPx() + scroll
            val relaY = y - offset.y
            if (0 >= relaY && relaY + (TASK_HOURS * HOUR_SIZE).dp.toPx() > 0) {
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override val drawFun: DrawScope.(Boolean, Float, Float, Float) -> Unit = { grabbed, textBuffer, daySize, scroll ->
        val color = if (grabbed) {
            color * DARKEN_PERCENT
        } else {
            color
        }

        val x = textBuffer + ((daySize + DAY_PADDING) * (task.dueTime.date.get(Calendar.DAY_OF_WEEK) - 1))
        val y = (HOUR_SIZE * task.dueTime.hour).dp.toPx() + scroll
        val height = (TASK_HOURS * HOUR_SIZE).dp.toPx()

        drawRoundRect(color, Offset(x, y), Size(daySize, height), CornerRadius(CORNER_RADIUS.dp.toPx()))
    }

    override val dragFun: PointerInputScope.(Offset, Float, Float, Float, Float) -> Unit = { change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = min(max(((-textBuffer + change.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS - 1)
        val hour = min(max(((grabbedOffset + change.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), 24f - TASK_HOURS)

        task.dueTime.date.set(Calendar.DAY_OF_WEEK, day + 1)
        task.dueTime.hour = hour
    }
}

class CalendarDummy : CalendarObject() {
    override val inBoundsFun: PointerInputScope.(Offset, Float, Float, Float) -> Boolean = { _, _, _, _ ->
        throw NotImplementedError()
    }

    override val drawFun: DrawScope.(Boolean, Float, Float, Float) -> Unit = { _, _, _, _ ->
        throw NotImplementedError()
    }

    override val dragFun: PointerInputScope.(Offset, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ ->
        throw NotImplementedError()
    }
}
