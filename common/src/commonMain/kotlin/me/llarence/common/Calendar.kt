package me.llarence.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

operator fun Color.times(value: Float): Color {
    return Color(red * value, green * value, blue * value)
}

fun SnapshotStateList<CalendarObject>.forceUpdateList() {
    this.add(0, CalendarDummy())
    this.removeAt(0)
}

// TODO: Check if timezone should be state
// TODO: Show calendarObjects covered by other calendarObjects
// TODO: Make this render based on calendar so special time rules apply (if necessary)
// The last event is always the selected one
// Needs dateState because it won't update the modifier without it
@OptIn(ExperimentalTextApi::class)
@Composable
fun RenderedCalendar(calendarObjects: SnapshotStateList<CalendarObject>, calendarEventsGenerated: SnapshotStateList<CalendarEvent>, weekInstantState: MutableState<Instant>, timeZone: TimeZone, onInteract: (CalendarObject) -> Unit, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    var scroll by remember { mutableStateOf(-HOUR_SIZE * START_HOUR) }

    var textBuffer by remember { mutableStateOf(0f) }
    var daySize by remember { mutableStateOf(0f) }

    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(0f) }

    val weekInstant by weekInstantState
    val date = weekInstant.toLocalDateTime(timeZone)

    if (date.nanosecond != 0 || date.second != 0 || date.minute != 0 || date.hour != 0) {
        throw IllegalArgumentException()
    }

    Canvas(
        modifier
        .clipToBounds()
        .scrollable(
            orientation = Orientation.Vertical,
            state = rememberScrollableState { delta ->
                scroll += SCROLL_SPEED * delta
                delta
            }
        )
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                scroll += (dragAmount.y / density).dp
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { startPos ->
                    dragging = false

                    // Make this repeated code into functions
                    for (i in (calendarObjects.size - 1) downTo 0) {
                        val calendarObject = calendarObjects[i]
                        val offset = calendarObject.inBounds(this, weekInstant, startPos, textBuffer, daySize, scroll)
                        if (offset != null) {
                            dragging = true

                            val index = calendarObjects.size - 1
                            calendarObjects[i] = calendarObjects[index]
                            calendarObjects[index] = calendarObject

                            dragOffset = offset

                            calendarObject.drag(this, weekInstant, startPos, dragOffset, textBuffer, daySize, scroll)
                            calendarObjects.forceUpdateList()

                            onInteract(calendarObject)

                            break
                        }
                    }

                    if (!dragging) {
                        for (i in (calendarEventsGenerated.size - 1) downTo 0) {
                            val calendarObject = calendarEventsGenerated[i]
                            val offset = calendarObject.inBounds(this, weekInstant, startPos, textBuffer, daySize, scroll)
                            if (offset != null) {
                                dragging = true

                                calendarEventsGenerated.removeAt(i)
                                calendarObjects.add(calendarObject)
                                calendarObject.generated = false

                                dragOffset = offset

                                calendarObject.drag(this, weekInstant, startPos, dragOffset, textBuffer, daySize, scroll)
                                calendarObjects.forceUpdateList()

                                onInteract(calendarObject)

                                break
                            }
                        }
                    }
                },

                onDragEnd = {
                    dragging = false
                },

                onDrag = { change, dragAmount ->
                    change.consume()

                    if (!dragging) {
                        scroll += (dragAmount.y / density).dp
                    } else {
                        val calendarObject = calendarObjects.last()
                        calendarObject.drag(this, weekInstant, change.position, dragOffset, textBuffer, daySize, scroll)
                        calendarObjects.forceUpdateList()

                        onInteract(calendarObject)
                    }
                }
            )
        }
    ) {
        drawRect(Color.Cyan)

        var textWidth = 0
        for (i in 0 until HOURS_IN_DAY) {
            val text = "$i.00"

            if (i == 0) {
                // Maybe having scroll changed here doesn't make a lot of sense
                val textMeasure = textMeasurer.measure(AnnotatedString(text))
                scroll = min(max(scroll, (size.height / density).dp - (HOUR_SIZE * HOURS_IN_DAY)), (textMeasure.size.height / density).dp / 2f)
            }

            val textMeasure = textMeasurer.measure(AnnotatedString(text))
            val y = (HOUR_SIZE * i).toPx() - textMeasure.size.height / 2f + scroll.toPx()
            if (size.height > y) {
                drawText(textMeasure, topLeft = Offset(0f, y))
            }

            if (textMeasure.size.width > textWidth) {
                textWidth = textMeasure.size.width
            }
        }

        val dayPadding = DAY_PADDING.toPx()
        textBuffer = textWidth + TEXT_PADDING.toPx()
        daySize = max((size.width + dayPadding - textBuffer) / DAYS_IN_WEEK - dayPadding, 0f)

        for (i in 0 until DAYS_IN_WEEK) {
            val startX = textBuffer + ((daySize + dayPadding) * i)
            drawRect(Color.LightGray, Offset(startX, scroll.toPx()), Size(daySize, (HOUR_SIZE * HOURS_IN_DAY).toPx()))
        }

        for (i in 0 until HOURS_IN_DAY) {
            val y = (HOUR_SIZE * i).toPx() + scroll.toPx()
            drawLine(Color.Gray, Offset(textBuffer, y), Offset(size.width, y))
        }

        for (i in calendarEventsGenerated.indices) {
            calendarEventsGenerated[i].preDraw(this, weekInstant, textBuffer, daySize, scroll, textMeasurer)
        }

        for (i in calendarObjects.indices) {
            calendarObjects[i].preDraw(this, weekInstant, textBuffer, daySize, scroll, textMeasurer)
        }

        for (i in calendarEventsGenerated.indices) {
            calendarEventsGenerated[i].draw(this, weekInstant, false, textBuffer, daySize, scroll, textMeasurer)
        }

        for (i in calendarObjects.indices) {
            calendarObjects[i].draw(this, weekInstant, dragging && i == calendarObjects.size - 1, textBuffer, daySize, scroll, textMeasurer)
        }

        for (i in calendarEventsGenerated.indices) {
            calendarEventsGenerated[i].postDraw(this, weekInstant, textBuffer, daySize, scroll, textMeasurer)
        }

        for (i in calendarObjects.indices) {
            calendarObjects[i].postDraw(this, weekInstant, textBuffer, daySize, scroll, textMeasurer)
        }
    }
}
