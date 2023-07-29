package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.util.*

// TODO: Check stuff to make sure it is using compose properly for performance
@Composable
fun app() {
    Row {
        val startDate = Calendar.getInstance()
        startDate.set(Calendar.DAY_OF_WEEK, 3)

        val calendarDate = Calendar.getInstance()

        val grabbedEventState = remember { mutableStateOf(false) }

        val events = remember { mutableStateListOf<Event>() }

        Column {
            val grabbedEvent by grabbedEventState
            var hasGrabbed by remember { mutableStateOf(false) }
            if (!hasGrabbed && grabbedEvent) {
                hasGrabbed = true
            }

            Button({
                val currData = Calendar.getInstance()
                events.add(Event(Time(currData, currData.get(Calendar.HOUR_OF_DAY).toFloat()), 1f, 0))
            }) {
                Text("New")
            }

            // TODO: Make the text based on what the user typed not a toString()
            TextField(if (hasGrabbed) { events.last().duration.toString() } else { "" }, {
                val res = it.toFloatOrNull()
                if (res != null) {
                    val index = events.size - 1
                    val event = events[index]
                    events[index] = Event(event.time, res, event.location)
                }
            }, enabled = hasGrabbed, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        RenderedCalendar(events, calendarDate, Modifier.fillMaxSize(), grabbedEventState)
    }
}
