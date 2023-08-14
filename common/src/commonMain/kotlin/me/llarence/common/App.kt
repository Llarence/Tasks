package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds

fun randomColor(): Color {
    return Color(Random.nextBits(8), Random.nextBits(8), Random.nextBits(8))
}

fun LocalDateTime.copy(year: Int = this.year, monthNumber: Int = this.monthNumber, dayOfMonth: Int = this.dayOfMonth, hour: Int = this.hour, minute: Int = this.minute, second: Int = this.second, nanosecond: Int = this.nanosecond): LocalDateTime {
    return LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
}

fun Instant.getFloatHour(timeZone: TimeZone): Float {
    return (this - toLocalDateTime(timeZone).copy(hour = 0, minute = 0, second = 0, nanosecond = 0).toInstant(timeZone)).inWholeNanoseconds * HOURS_IN_NANO
}

fun Instant.withFloatHour(value: Float, timeZone: TimeZone): Instant {
    return toLocalDateTime(timeZone).copy(hour = 0, minute = 0, second = 0, nanosecond = 0).toInstant(timeZone) + (value * NANOS_IN_HOUR).toLong().nanoseconds
}

// TODO: Check stuff to make sure it complies with compose
// TODO: Add removing (it will mean has grabbed has to be set so false and renamed)
// TODO: Cache toLocalDatetime
@Composable
fun app() {
    val timeZone = remember { TimeZone.currentSystemDefault() }

    Row {
        val calendarObjects = remember { mutableStateListOf<CalendarObject>() }

        Column {
            Button({
                calendarObjects.add(0, CalendarEvent(Event(Clock.System.now(), DEFAULT_HOURS, 0, null), randomColor()))
            }) {
                Text("New Event")
            }

            Button({
                calendarObjects.add(0, CalendarTask(Task(DEFAULT_HOURS, mutableListOf(), mutableListOf(), mutableListOf(), Clock.System.now(), null), randomColor()))
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

            var taskSelected by remember { mutableStateOf<Task?>(null) }
            val checked = taskSelected != null
            if (checked) {
                if (last is CalendarEvent) {
                    if (taskSelected!!.event == last.event) {
                        taskSelected!!.event = null
                        last.event.task = null
                    } else {
                        taskSelected!!.event?.task = null
                        last.event.task?.event = null

                        taskSelected!!.event = last.event
                        last.event.task = taskSelected
                    }

                    calendarObjects.forceUpdate()
                    taskSelected = null
                } else if (last is CalendarTask && last.task != taskSelected) {
                    if (taskSelected!!.requirements.contains(last.task)) {
                        taskSelected!!.requirements.remove(last.task)
                        last.task.requiredFor.remove(taskSelected!!)
                    } else {
                        taskSelected!!.requiredFor.remove(last.task)
                        last.task.requirements.remove(taskSelected!!)

                        taskSelected!!.requirements.add(last.task)
                        last.task.requiredFor.add(taskSelected!!)
                    }

                    calendarObjects.forceUpdate()
                    taskSelected = null
                }
            }

            // TODO: Stop crashing on invalid values
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
                last.event.time.getFloatHour(timeZone)
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.getFloatHour(timeZone)
            } else {
                0f
            }

            RestrictedTextField(hour, Float::toString, String::toFloatOrNull, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time.withFloatHour(it, timeZone)
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime.withFloatHour(it, timeZone)
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent || isTask, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            val day = if (isEvent) {
                last as CalendarEvent
                last.event.time.toLocalDateTime(timeZone).dayOfMonth
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.toLocalDateTime(timeZone).dayOfMonth
            } else {
                0
            }

            RestrictedTextField(day, Int::toString, String::toIntOrNull, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time = last.event.time.toLocalDateTime(timeZone).copy(dayOfMonth = it).toInstant(timeZone)
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime = last.task.dueTime.toLocalDateTime(timeZone).copy(dayOfMonth = it).toInstant(timeZone)
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            val month = if (isEvent) {
                last as CalendarEvent
                last.event.time.toLocalDateTime(timeZone).monthNumber
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.toLocalDateTime(timeZone).monthNumber
            } else {
                0
            }

            RestrictedTextField(month, Int::toString, String::toIntOrNull, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time = last.event.time.toLocalDateTime(timeZone).copy(monthNumber = it).toInstant(timeZone)
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime = last.task.dueTime.toLocalDateTime(timeZone).copy(monthNumber = it).toInstant(timeZone)
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            val year = if (isEvent) {
                last as CalendarEvent
                last.event.time.toLocalDateTime(timeZone).year
            } else if (isTask) {
                last as CalendarTask
                last.task.dueTime.toLocalDateTime(timeZone).year
            } else {
                0
            }

            RestrictedTextField(year, Int::toString, String::toIntOrNull, {
                if (isEvent) {
                    last as CalendarEvent
                    last.event.time = last.event.time.toLocalDateTime(timeZone).copy(year = it).toInstant(timeZone)
                } else if (isTask) {
                    last as CalendarTask
                    last.task.dueTime = last.task.dueTime.toLocalDateTime(timeZone).copy(year = it).toInstant(timeZone)
                }
                calendarObjects.forceUpdate()
            }, enabled = isEvent, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        Column {
            val calendarTimeState = remember {
                val datetime = Clock.System.now().toLocalDateTime(timeZone)
                mutableStateOf(datetime.copy(hour = 0, minute = 0, second = 0, nanosecond = 0).toInstant(timeZone))
            }

            var calendarTime by calendarTimeState

            // TODO: See if there is way to modify the date without copying
            Row {
                Button({
                    calendarTime -= 7.days
                }) {
                    Text("Previous Week")
                }

                Button({
                    calendarTime += 7.days
                }) {
                    Text("Next Week")
                }

                val calendarDatetime = calendarTime.toLocalDateTime(timeZone)
                Text("${calendarDatetime.month} ${calendarDatetime.dayOfMonth}, ${calendarDatetime.year}")
            }

            RenderedCalendar(calendarObjects, calendarTimeState, timeZone, Modifier.fillMaxSize())
        }
    }
}
