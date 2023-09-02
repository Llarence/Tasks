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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlinx.datetime.Instant
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds

// This code is kinda dumb, but it is all hidden in this file
// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage
// TODO: Look into making it more optimized
// TODO: Move some of the math into functions

// Task and Event are not in the json
abstract class CalendarObject {
    // Kinda dumb but I want the scope mostly for .toPx()
    // It feels like less should be passed into these functions
    abstract val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Dp) -> Float?
    @OptIn(ExperimentalTextApi::class)
    abstract val preDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit
    @OptIn(ExperimentalTextApi::class)
    abstract val drawFun: DrawScope.(Instant, Boolean, Float, Float, Dp, TextMeasurer) -> Unit
    @OptIn(ExperimentalTextApi::class)
    abstract val postDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit
    abstract val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Dp) -> Unit

    abstract fun toJson(): JSONObject

    fun inBounds(pointerInputScope: PointerInputScope, time: Instant, offset: Offset, textBuffer: Float, daySize: Float, scroll: Dp): Float? {
        return inBoundsFun(pointerInputScope, time, offset, textBuffer, daySize, scroll)
    }

    @OptIn(ExperimentalTextApi::class)
    fun preDraw(drawScope: DrawScope, time: Instant, textBuffer: Float, daySize: Float, scroll: Dp, textMeasurer: TextMeasurer) {
        preDrawFun(drawScope, time, textBuffer, daySize, scroll, textMeasurer)
    }

    @OptIn(ExperimentalTextApi::class)
    fun draw(drawScope: DrawScope, time: Instant, grabbed: Boolean, textBuffer: Float, daySize: Float, scroll: Dp, textMeasurer: TextMeasurer) {
        drawFun(drawScope, time, grabbed, textBuffer, daySize, scroll, textMeasurer)
    }

    @OptIn(ExperimentalTextApi::class)
    fun postDraw(drawScope: DrawScope, time: Instant, textBuffer: Float, daySize: Float, scroll: Dp, textMeasurer: TextMeasurer) {
        postDrawFun(drawScope, time, textBuffer, daySize, scroll, textMeasurer)
    }

    fun drag(pointerInputScope: PointerInputScope, time: Instant, change: Offset, grabbedOffset: Float, textBuffer: Float, daySize: Float, scroll: Dp) {
        dragFun(pointerInputScope, time, change, grabbedOffset, textBuffer, daySize, scroll)
    }
}

abstract class MetaDataCalendarObject(var title: String, var description: String, var color: Color) : CalendarObject() {
    override fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("title", title)
        json.put("description", description)
        json.put("color", color.value.toLong())

        return json
    }
}

class CalendarEvent(val event: Event, var generated: Boolean, title: String, description: String, color: Color) : MetaDataCalendarObject(title, description, color) {
    constructor(event: Event, json: JSONObject) : this(event, json.getBoolean("generated"), json.getString("title"), json.getString("description"), Color(json.getLong("color").toULong()))

    val inBoundsSingleFun: PointerInputScope.(Duration, Duration, Offset, Float, Float, Dp) -> Float? = { startDiff, endDiff, offset, textBuffer, daySize, scroll ->
        var ret: Float? = null
        for (day in startDiff.inWholeDays..endDiff.inWholeDays) {
            val dayPadding = DAY_PADDING.toPx()
            val x = textBuffer + ((daySize + dayPadding) * day)

            if (offset.x in x..(x + daySize)) {
                val startDiffForDay = startDiff - day.days
                val startY = (HOUR_SIZE * max(startDiffForDay.inWholeNanoseconds * HOURS_PER_NANO, 0f) + scroll).toPx()

                val endDiffForDay = endDiff - day.days
                val endY = (HOUR_SIZE * min(endDiffForDay.inWholeNanoseconds * HOURS_PER_NANO, HOURS_IN_DAY.toFloat()) + scroll).toPx()

                if (offset.y in startY..endY) {
                    val mouseDay = ((-textBuffer + offset.x) / (daySize + dayPadding) - 0.5f).roundToInt()
                    val hourSizePx = HOUR_SIZE.toPx()
                    val mouseHour = (offset.y - scroll.toPx()) / hourSizePx

                    ret = (startDiff - mouseDay.days - (mouseHour * NANOS_PER_HOUR).toLong().nanoseconds).inWholeNanoseconds * HOURS_PER_NANO * hourSizePx
                    break
                }
            }
        }

        ret
    }

    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Dp) -> Float? = { time, offset, textBuffer, daySize, scroll ->
        if (event.repeat == null) {
            val startDiff = event.time - time
            val endDiff = startDiff + event.duration
            if (startDiff < 7.days && endDiff >= 0.days) {
                inBoundsSingleFun(startDiff, endDiff, offset, textBuffer, daySize, scroll)
            } else {
                null
            }
        } else {
            val ret: Float?

            val diff = event.time - time
            val repeats = (diff / event.repeat!!).toInt()
            // TODO: Check this if (repeats >= 0) { 0.nanoseconds } else { event.repeat!! }
            var startDiff = diff - (event.repeat!! * repeats) + if (repeats >= 0) { 0.nanoseconds } else { event.repeat!! }

            while (true) {
                val endDiff = startDiff + event.duration
                if (startDiff < 7.days && endDiff >= 0.days) {
                    val currRet = inBoundsSingleFun(startDiff, endDiff, offset, textBuffer, daySize, scroll)
                    if (currRet != null) {
                        ret = currRet
                        break
                    }
                } else {
                    ret = null
                    break
                }

                startDiff += event.repeat!!
            }

            ret
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override val preDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { _, _, _, _, _ ->
    }

    val drawSingleFun: DrawScope.(Duration, Duration, Boolean, Float, Float, Dp) -> Unit = { startDiff, endDiff, grabbed, textBuffer, daySize, scroll ->
        val currColor = if (grabbed) {
            this@CalendarEvent.color * DARKEN_PERCENT
        } else {
            this@CalendarEvent.color
        }

        for (day in startDiff.inWholeDays..endDiff.inWholeDays) {
            if (day !in 0 until 7) {
                continue
            }

            val x = textBuffer + ((daySize + DAY_PADDING.toPx()) * day)

            val startDiffForDay = startDiff - day.days
            val startY = (HOUR_SIZE * max(startDiffForDay.inWholeNanoseconds * HOURS_PER_NANO, 0f) + scroll).toPx()

            val endDiffForDay = endDiff - day.days
            val endY = (HOUR_SIZE * min(endDiffForDay.inWholeNanoseconds * HOURS_PER_NANO, HOURS_IN_DAY.toFloat()) + scroll).toPx()

            drawRoundRect(currColor, Offset(x, startY), Size(daySize, endY - startY), CornerRadius(CORNER_RADIUS.toPx()))
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Dp, TextMeasurer) -> Unit = { time, grabbed, textBuffer, daySize, scroll, _ ->
        if (event.repeat == null) {
            val startDiff = event.time - time
            val endDiff = startDiff + event.duration
            if (startDiff < 7.days && endDiff >= 0.days) {
                drawSingleFun(startDiff, endDiff, grabbed, textBuffer, daySize, scroll)
            }
        } else {
            val diff = event.time - time
            val repeats = (diff / event.repeat!!).toInt()
            var startDiff = diff - (event.repeat!! * repeats) + if (repeats >= 0) { 0.nanoseconds } else { event.repeat!! }

            while (true) {
                val endDiff = startDiff + event.duration
                if (startDiff < 7.days && endDiff >= 0.days) {
                    drawSingleFun(startDiff, endDiff, grabbed, textBuffer, daySize, scroll)
                } else {
                    break
                }

                startDiff += event.repeat!!
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override val postDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { time, textBuffer, daySize, scroll, textMeasurer ->
        val diff = event.time - time
        if (diff < 7.days && diff >= 0.days) {
            val x = textBuffer + ((daySize + DAY_PADDING.toPx()) * diff.inWholeDays) + daySize / 2f
            val dayDiff = diff - diff.inWholeDays.days
            val y = (HOUR_SIZE * (dayDiff.inWholeNanoseconds * HOURS_PER_NANO) + scroll).toPx()

            val res = textMeasurer.measure(AnnotatedString(this@CalendarEvent.title), overflow = TextOverflow.Ellipsis, softWrap = false, constraints = Constraints(0, daySize.toInt(), 0, Constraints.Infinity))

            drawText(res, Color.Black, Offset(x - (res.size.width / 2f), y - res.size.height))
        }
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Dp) -> Unit = { time, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = ((-textBuffer + change.x) / (daySize + DAY_PADDING.toPx()) - 0.5f).roundToInt()
        val hour = (grabbedOffset + change.y - scroll.toPx()) / HOUR_SIZE.toPx()

        event.time = time + day.days + (hour * NANOS_PER_HOUR).toLong().nanoseconds
    }

    override fun toJson(): JSONObject {
        val json = super.toJson()

        json.put("generated", generated)

        return json
    }
}

class CalendarTask(val task: Task, title: String, description: String, color: Color) : MetaDataCalendarObject(title, description, color) {
    constructor(task: Task, json: JSONObject) : this(task, json.getString("title"), json.getString("description"), Color(json.getLong("color").toULong()))

    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Dp) -> Float? = { time, offset, textBuffer, daySize, scroll ->
        val diff = task.dueTime - time
        if (diff < 7.days && diff >= 0.days) {
            val x = textBuffer + ((daySize + DAY_PADDING.toPx()) * diff.inWholeDays)
            val relaX = x - offset.x
            if (0 >= relaX && relaX + daySize > 0) {
                val dayDiff = diff - diff.inWholeDays.days
                val y = (HOUR_SIZE * (dayDiff.inWholeNanoseconds * HOURS_PER_NANO) + scroll).toPx()
                val relaY = y - offset.y
                if (0 >= relaY && relaY + (HOUR_SIZE * TASK_HOURS).toPx() > 0) {
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

    val pathTo: DrawScope.(Float, Float, Long, Float, Float, Float, Dp, PathEffect) -> Unit = { xFrom, yFrom, days, hours, textBuffer, daySize, scroll, pathEffect ->
        val xTo = textBuffer + ((daySize + DAY_PADDING.toPx()) * days) + daySize / 2f
        val yTo = ((HOUR_SIZE * hours) + scroll).toPx()

        val path = Path()
        path.moveTo(xFrom, yFrom)
        path.cubicTo(xTo, yFrom + PATH_STRENGTH.toPx(), xFrom, yTo - PATH_STRENGTH.toPx(), xTo, yTo)
        drawPath(path, color, style = Stroke(pathEffect = pathEffect))
    }

    @OptIn(ExperimentalTextApi::class)
    override val preDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { time, textBuffer, daySize, scroll, _ ->
        val diff = task.dueTime - time
        if (diff < 7.days && diff >= 0.days) {
            val xFrom = textBuffer + ((daySize + DAY_PADDING.toPx()) * diff.inWholeDays) + daySize / 2f
            val dayDiff = diff - diff.inWholeDays.days
            val yFrom = (HOUR_SIZE * (dayDiff.inWholeNanoseconds * HOURS_PER_NANO) + scroll).toPx() + (HOUR_SIZE * TASK_HOURS).toPx()

            val interval = PATH_INTERVALS.toPx()
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval))
            if (task.event != null) {
                val eventDiff = task.event!!.time - time
                if (eventDiff < 7.days) {
                    val eventDayDiff = eventDiff - eventDiff.inWholeDays.days
                    pathTo(this, xFrom, yFrom, eventDiff.inWholeDays, eventDayDiff.inWholeNanoseconds * HOURS_PER_NANO, textBuffer, daySize, scroll, pathEffect)
                }
            }

            for (requiredTask in task.requirements) {
                val taskDiff = requiredTask.dueTime - time
                if (taskDiff < 7.days) {
                    val taskDayDiff = taskDiff - taskDiff.inWholeDays.days
                    pathTo(this, xFrom, yFrom, taskDiff.inWholeDays, taskDayDiff.inWholeNanoseconds * HOURS_PER_NANO, textBuffer, daySize, scroll, pathEffect)
                }
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Dp, TextMeasurer) -> Unit = { time, grabbed, textBuffer, daySize, scroll, _ ->
        val diff = task.dueTime - time
        if (diff < 7.days && diff >= 0.days) {
            val currColor = if (grabbed) {
                this@CalendarTask.color * DARKEN_PERCENT
            } else {
                this@CalendarTask.color
            }

            val x = textBuffer + ((daySize + DAY_PADDING.toPx()) * diff.inWholeDays)
            val dayDiff = diff - diff.inWholeDays.days
            val y = (HOUR_SIZE * (dayDiff.inWholeNanoseconds * HOURS_PER_NANO) + scroll).toPx()
            val height = (HOUR_SIZE * TASK_HOURS).toPx()

            val triangle = Path()
            triangle.moveTo(x, y)
            triangle.relativeLineTo(daySize, 0f)
            triangle.relativeLineTo(-daySize / 2f, height)
            drawPath(triangle, currColor)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    override val postDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { time, textBuffer, daySize, scroll, textMeasurer ->
        val diff = task.dueTime - time
        if (diff < 7.days && diff >= 0.days) {
            val x = textBuffer + ((daySize + DAY_PADDING.toPx()) * diff.inWholeDays) + daySize / 2f
            val dayDiff = diff - diff.inWholeDays.days
            val y = ((HOUR_SIZE * (dayDiff.inWholeNanoseconds * HOURS_PER_NANO)) + scroll).toPx()

            val res = textMeasurer.measure(AnnotatedString(this@CalendarTask.title), overflow = TextOverflow.Ellipsis, softWrap = false, constraints = Constraints(0, daySize.toInt(), 0, Constraints.Infinity))

            drawText(res, Color.Black, Offset(x - (res.size.width / 2f), y - res.size.height))
        }
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Dp) -> Unit = { time, change, grabbedOffset, textBuffer, daySize, scroll ->
        val day = ((-textBuffer + change.x) / (daySize + DAY_PADDING.toPx()) - 0.5f).roundToInt()
        val hour = (grabbedOffset + change.y - scroll.toPx()) / HOUR_SIZE.toPx()

        task.dueTime = time + day.days + (hour * NANOS_PER_HOUR).toLong().nanoseconds
    }
}

class CalendarDummy : CalendarObject() {
    override val inBoundsFun: PointerInputScope.(Instant, Offset, Float, Float, Dp) -> Float? = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    @OptIn(ExperimentalTextApi::class)
    override val preDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    @OptIn(ExperimentalTextApi::class)
    override val drawFun: DrawScope.(Instant, Boolean, Float, Float, Dp, TextMeasurer) -> Unit = { _, _, _, _, _, _ ->
        throw NotImplementedError()
    }

    @OptIn(ExperimentalTextApi::class)
    override val postDrawFun: DrawScope.(Instant, Float, Float, Dp, TextMeasurer) -> Unit = { _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override val dragFun: PointerInputScope.(Instant, Offset, Float, Float, Float, Dp) -> Unit = { _, _, _, _, _, _ ->
        throw NotImplementedError()
    }

    override fun toJson(): JSONObject {
        throw NotImplementedError()
    }
}
