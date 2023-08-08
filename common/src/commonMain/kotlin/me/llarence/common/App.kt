package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*
import kotlin.random.Random

fun randomColor(): Color {
    return Color(Random.nextBits(8), Random.nextBits(8), Random.nextBits(8))
}

// TODO: Check stuff to make sure it complies with compose
// TODO: Add removing (it will mean has grabbed has to be set so false and renamed)
@Composable
fun app() {
    Row {
        val calendarObjects = remember { mutableStateListOf<CalendarObject>() }

        Column {
            Button({
                val currData = Calendar.getInstance()
                calendarObjects.add(0, CalendarEvent(Event(Time(currData, currData.get(Calendar.HOUR_OF_DAY).toFloat()), DEFAULT_HOURS, 0, null), randomColor()))
            }) {
                Text("New Event")
            }

            Button({
                val currData = Calendar.getInstance()
                calendarObjects.add(0, CalendarTask(Task(DEFAULT_HOURS, mutableListOf(), mutableListOf(), mutableListOf(), Time(currData, currData.get(Calendar.HOUR_OF_DAY).toFloat()), null), randomColor()))
            }) {
                Text("New Task")
            }

            val last = calendarObjects.lastOrNull()
            val isEvent = last is CalendarEvent
            val duration = if (isEvent) {
                last as CalendarEvent
                last.event.duration
            } else {
                0f
            }

            RestrictedTextField(duration, Float::toString, String::toFloatOrNull, {
                last as CalendarEvent
                last.event.duration = it
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            var r by remember { mutableStateOf(0f) }
            var g by remember { mutableStateOf(0f) }
            var b by remember { mutableStateOf(0f) }
            if (last is ColorableCalendarObject) {
                r = last.color.red
                g = last.color.green
                b = last.color.blue
            }

            Slider(r, {
                r = it
                (last as ColorableCalendarObject).color = Color(r, g, b)
                calendarObjects.forceUpdate()
            }, Modifier.size(100.dp), last is ColorableCalendarObject, colors = SliderDefaults.colors(thumbColor = Color(r, 0f, 0f)))

            Slider(g, {
                g = it
                (last as ColorableCalendarObject).color = Color(r, g, b)
                calendarObjects.forceUpdate()
            }, Modifier.size(100.dp), last is ColorableCalendarObject, colors = SliderDefaults.colors(thumbColor = Color(0f, g, 0f)))

            Slider(b, {
                b = it
                (last as ColorableCalendarObject).color = Color(r, g, b)
                calendarObjects.forceUpdate()
            }, Modifier.size(100.dp), last is ColorableCalendarObject, colors = SliderDefaults.colors(thumbColor = Color(0f, 0f, b)))

            Text("Set Event/Requirement")

            var taskSelected by remember { mutableStateOf<Task?>(null) }
            val checked = taskSelected != null
            if (checked) {
                if (last is CalendarEvent) {
                    taskSelected!!.event = last.event
                    last.event.task = taskSelected
                    calendarObjects.forceUpdate()

                    taskSelected = null
                } else if (last is CalendarTask && last.task != taskSelected) {
                    taskSelected!!.requirements.add(last.task)
                    last.task.requiredFor.add(taskSelected!!)
                    calendarObjects.forceUpdate()

                    taskSelected = null
                }
            }

            Switch(checked, {
                if (it) {
                    if (last is CalendarTask) {
                        taskSelected = last.task
                    }
                } else {
                    taskSelected = null
                }
            })

            val isTask = last is CalendarTask
            val hour = if (isEvent) {
                last as CalendarEvent
                last.event.time.hour
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.hour
            } else {
                0f
            }

            RestrictedTextField(hour, Float::toString, {
                val float = it.toFloatOrNull()
                if (float == null) {
                    null
                } else {
                    if (float >= 0f && float <= 24f - duration) {
                        float
                    } else {
                        null
                    }
                }
            }, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time.hour = it
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime.hour = it
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            val day = if (isEvent) {
                last as CalendarEvent
                last.event.time.date.get(Calendar.DAY_OF_WEEK)
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.date.get(Calendar.DAY_OF_WEEK)
            } else {
                0
            }

            RestrictedTextField(day, Int::toString, {
                val int = it.toIntOrNull()
                if (int == null) {
                    null
                } else {
                    if (int in 1..7) {
                        int
                    } else {
                        null
                    }
                }
            }, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time.date.set(Calendar.DAY_OF_WEEK, it)
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime.date.set(Calendar.DAY_OF_WEEK, it)
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        Column {
            val calendarDate by remember { mutableStateOf(Calendar.getInstance()) }

            Row {
                Button({
                    calendarDate.add(Calendar.WEEK_OF_YEAR, -1)
                }) {
                    Text("Previous Week")
                }

                Button({
                    calendarDate.add(Calendar.WEEK_OF_YEAR, 1)
                }) {
                    Text("Next Week")
                }
            }

            RenderedCalendar(calendarObjects, calendarDate, Modifier.fillMaxSize())
        }
    }
}
