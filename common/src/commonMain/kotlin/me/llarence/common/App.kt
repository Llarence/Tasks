package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.random.Random

fun randomColor(): Color {
    return Color(Random.nextBits(8), Random.nextBits(8), Random.nextBits(8))
}

// TODO: Check stuff to make sure it is using compose properly for performance
// TODO: Add removing (it will mean has grabbed has to be set so false and renamed)
@Composable
fun app() {
    Row {
        val startDate = Calendar.getInstance()
        startDate.set(Calendar.DAY_OF_WEEK, 3)

        val calendarDate = Calendar.getInstance()

        val grabbedEventState = remember { mutableStateOf(false) }

        val calendarObjects = remember { mutableStateListOf<CalendarObject>() }

        Column {
            val grabbed by grabbedEventState
            var hasGrabbed by remember { mutableStateOf(false) }
            if (!hasGrabbed && grabbed) {
                hasGrabbed = true
            }

            Button({
                val currData = Calendar.getInstance()
                calendarObjects.add(CalendarEvent(Event(Time(currData, currData.get(Calendar.HOUR_OF_DAY).toFloat()), DEFAULT_HOURS, 0), randomColor()))
            }) {
                Text("New Event")
            }

            Button({
                val currData = Calendar.getInstance()
                calendarObjects.add(CalendarTask(Task(DEFAULT_HOURS, listOf(), listOf(), Time(currData, currData.get(Calendar.HOUR_OF_DAY).toFloat())), randomColor()))
            }) {
                Text("New Task")
            }

            var text by remember { mutableStateOf("") }

            val last = calendarObjects.lastOrNull()
            val isEvent = last is CalendarEvent
            if (isEvent && grabbed) {
                val res = text.toFloatOrNull()
                if (res != last!!.duration) {
                    text = last.duration.toString()
                }
            }

            TextField(text, {
                text = it

                val res = it.toFloatOrNull()
                if (res != null) {
                    calendarObjects[calendarObjects.size - 1] = (last as CalendarEvent).createWithDuration(res)
                }
            }, enabled = hasGrabbed && isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            var r by remember { mutableStateOf(0f) }
            var g by remember { mutableStateOf(0f) }
            var b by remember { mutableStateOf(0f) }
            if (grabbed) {
                r = last!!.color.red
                g = last.color.green
                b = last.color.blue
            }

            Slider(r, {
                r = it
                calendarObjects[calendarObjects.size - 1] = last!!.createWithNewColor(Color(r, g, b))
            }, Modifier.size(100.dp), hasGrabbed, colors = SliderDefaults.colors(thumbColor = Color(r, 0f, 0f)))

            Slider(g, {
                g = it
                calendarObjects[calendarObjects.size - 1] = last!!.createWithNewColor(Color(r, g, b))
            }, Modifier.size(100.dp), hasGrabbed, colors = SliderDefaults.colors(thumbColor = Color(0f, g, 0f)))

            Slider(b, {
                b = it
                calendarObjects[calendarObjects.size - 1] = last!!.createWithNewColor(Color(r, g, b))
            }, Modifier.size(100.dp), hasGrabbed, colors = SliderDefaults.colors(thumbColor = Color(0f, 0f, b)))
        }

        RenderedCalendar(calendarObjects, calendarDate, Modifier.fillMaxSize(), grabbedEventState)
    }
}
