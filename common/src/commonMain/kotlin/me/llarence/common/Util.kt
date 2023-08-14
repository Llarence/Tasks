package me.llarence.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun <T> RestrictedTextField(value: T, valueToText: (T) -> String, textToValue: (String) -> T?, onValueChange: (T) -> Unit, enabled: Boolean = true, keyboardOptions: KeyboardOptions = KeyboardOptions()) {
    var text by remember { mutableStateOf<String?>(null) }

    if (text == null) {
        text = valueToText(value)
    } else {
        val textValue = textToValue(text!!)
        if (textValue != null) {
            if (textValue != value) {
                text = valueToText(value)
            }
        }
    }

    TextField(text!!, {
        text = it

        val currValue = textToValue(text!!)
        if (currValue != null) {
            onValueChange(currValue)
        }
    }, enabled = enabled, keyboardOptions = keyboardOptions)
}

fun calendarObjectsToEventsAndTasks(calendarObjects: List<CalendarObject>): Pair<List<Event>, List<Task>> {
    val events = mutableListOf<Event>()
    val tasks = mutableListOf<Task>()

    for (calendarObject in calendarObjects) {
        if (calendarObject is CalendarEvent) {
            events.add(calendarObject.event)
        } else if (calendarObject is CalendarTask) {
            tasks.add(calendarObject.task)
        } else {
            throw IllegalArgumentException()
        }
    }

    return Pair(events, tasks)
}
