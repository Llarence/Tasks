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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathBuilder
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

fun SnapshotStateList<CalendarObject>.forceUpdate() {
    this.add(CalendarDummy())
    this.removeAt(this.size - 1)
}

// TODO: Show calendarObjects covered by other calendarObjects
// TODO: Move most of the RenderCalender code into separate functions
// The last event is always the selected one
@OptIn(ExperimentalTextApi::class)
@Composable
fun RenderedCalendar(calendarObjects: SnapshotStateList<CalendarObject>, date: Calendar, modifier: Modifier = Modifier, grabbedDelegate: MutableState<Boolean> = mutableStateOf(false)) {
    val textMeasurer = rememberTextMeasurer()

    var scroll by remember { mutableStateOf(Float.POSITIVE_INFINITY) }

    var textBuffer by remember { mutableStateOf(0f) }
    var daySize by remember { mutableStateOf(0f) }

    var grabbed by grabbedDelegate
    var grabbedOffset by remember { mutableStateOf(0f) }

    Canvas(
        modifier
        .clipToBounds()
        .scrollable(
            orientation = Orientation.Vertical,
            state = rememberScrollableState { delta ->
                scroll += delta * SCROLL_SPEED
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
                    grabbed = false

                    for (i in (calendarObjects.size - 1) downTo 0) {
                        val calendarObject = calendarObjects[i]
                        if (calendarObject.inBounds(this, startPos, textBuffer, daySize, scroll)) {
                            grabbed = true

                            val index = calendarObjects.size - 1
                            calendarObjects[i] = calendarObjects[index]
                            calendarObjects[index] = calendarObject

                            grabbedOffset = 0f
                            break
                        }
                    }
                },

                onDragEnd = {
                    grabbed = false
                },

                onDrag = { change, dragAmount ->
                    change.consume()

                    if (!grabbed) {
                        scroll += dragAmount.y
                    } else {
                        val calendarObject = calendarObjects.lastOrNull()
                        if (calendarObject != null) {
                            calendarObject.drag(this, change.position, grabbedOffset, textBuffer, daySize, scroll)
                            calendarObjects.forceUpdate()
                        }
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

        for (i in calendarObjects.indices) {
            val calendarObject = calendarObjects[i]
            calendarObject.draw(this, grabbed, textBuffer, daySize, scroll)
        }
    }
}
