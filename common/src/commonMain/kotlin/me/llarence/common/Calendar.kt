package me.llarence.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Calendar

operator fun Color.times(value: Float): Color {
    return Color(red * value, green * value, blue * value)
}

// TODO: Show events covered by other events
// TODO: Move some of the repeated math into functions
@OptIn(ExperimentalTextApi::class)
@Composable
fun RenderedCalendar(events: SnapshotStateList<Event>, date: Calendar, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    var scroll by mutableStateOf(Float.POSITIVE_INFINITY)

    var textBuffer by mutableStateOf(0f)
    var daySize by mutableStateOf(0f)

    var grabbedEvent by mutableStateOf(false)
    var grabbedOffset by mutableStateOf(0f)

    Canvas(
        modifier
        .clipToBounds()
        .scrollable(
            orientation = Orientation.Vertical,
            state = rememberScrollableState { delta ->
                scroll += delta
                delta
            }
        )
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                scroll += dragAmount.y
            }
        }
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { startPos ->
                    grabbedEvent = false

                    for (i in (events.size - 1) downTo 0) {
                        val event = events[i]
                        if (!(event.time.date.get(Calendar.YEAR) == date.get(Calendar.YEAR) && event.time.date.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR))) {
                            continue
                        }

                        val x = textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1))
                        val relaX = x - startPos.x
                        if (0 >= relaX && relaX + daySize > 0) {
                            val y = (event.time.hour * HOUR_SIZE).dp.toPx() + scroll
                            val relaY = y - startPos.y
                            if (0 >= relaY && relaY + (event.duration * HOUR_SIZE).dp.toPx() > 0) {
                                grabbedEvent = true

                                val index = events.size - 1
                                events[i] = events[index]
                                events[index] = event

                                grabbedOffset = relaY
                                break
                            }
                        }
                    }
                },

                onDragEnd = {
                    grabbedEvent = false
                },

                onDrag = { change, dragAmount ->
                    change.consume()

                    if (!grabbedEvent) {
                        scroll += dragAmount.y
                    } else {
                        val index = events.size - 1
                        val event = events[index]

                        val day = min(max(((-textBuffer + change.position.x) / (daySize + DAY_PADDING) - 0.5f).roundToInt(), 0), DAYS - 1)
                        val hour = min(max(((grabbedOffset + change.position.y - scroll) / HOUR_SIZE / HOUR_SNAP).roundToInt() * HOUR_SNAP, 0f), 24f - event.duration)

                        // Maybe this should be copied for simplicity
                        event.time.date.set(Calendar.DAY_OF_WEEK, day + 1)
                        events[index] = Event(Time(event.time.date, hour), event.duration, event.location)
                    }
                }
            )
        }
    ) {
        drawRect(Color.Cyan)

        var textWidth = 0
        for (i in 0 until HOURS) {
            val text = "$i.00"

            if (i == 0) {
                // Maybe having scroll changed here doesn't make a lot of sense
                val textMeasure = textMeasurer.measure(AnnotatedString(text))
                scroll = min(max(scroll, -(HOURS * HOUR_SIZE).dp.toPx() + size.height), textMeasure.size.height / 2f)
            }

            val textMeasure = textMeasurer.measure(AnnotatedString(text))
            val y = (HOUR_SIZE * i).dp.toPx() - textMeasure.size.height / 2f + scroll
            if (size.height > y) {
                drawText(textMeasurer, text, Offset(0f, y))
            }

            if (textMeasure.size.width > textWidth) {
                textWidth = textMeasure.size.width
            }
        }

        textBuffer = textWidth + TEXT_PADDING
        daySize = max((size.width + DAY_PADDING - textBuffer) / DAYS - DAY_PADDING, 0f)

        for (i in 0 until DAYS) {
            val startX = textBuffer + ((daySize + DAY_PADDING) * i)
            drawRect(Color.LightGray, Offset(startX.dp.toPx(), scroll), Size(daySize.dp.toPx(), (HOURS * HOUR_SIZE).dp.toPx()))
        }

        for (i in 0 until HOURS) {
            val y = (HOUR_SIZE * i).dp.toPx() + scroll
            drawLine(Color.Gray, Offset(textBuffer, y), Offset(size.width, y))
        }

        for (i in events.indices) {
            val event = events[i]
            if (grabbedEvent && i == events.size - 1) {
                drawRoundRect(Color.Green * GRAB_DARKEN_PERCENT, Offset(textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1)), (HOUR_SIZE * event.time.hour).dp.toPx() + scroll), Size(daySize, (event.duration * HOUR_SIZE).dp.toPx()), CornerRadius(CORNER_RADIUS.dp.toPx()))
            } else {
                drawRoundRect(Color.Green, Offset(textBuffer + ((daySize + DAY_PADDING) * (event.time.date.get(Calendar.DAY_OF_WEEK) - 1)), (HOUR_SIZE * event.time.hour).dp.toPx() + scroll), Size(daySize, (event.duration * HOUR_SIZE).dp.toPx()), CornerRadius(CORNER_RADIUS.dp.toPx()))
            }
        }
    }
}
