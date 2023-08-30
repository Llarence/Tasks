package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

// TODO: Check to make sure that everything works with random recomposes?
// TODO: Add removing
@Composable
fun app() {
    var eventUpdateFun by remember { mutableStateOf<(CalendarObject) -> Unit>({  }) }
    var taskUpdateFun by remember { mutableStateOf<(CalendarObject) -> Unit>({  }) }
    val timeZone = remember { TimeZone.currentSystemDefault() }

    Row {
        val calendarObjects = remember { mutableStateListOf<CalendarObject>() }

        Column(Modifier.width(200.dp)) {
            Button({
                calendarObjects.add(0, CalendarEvent(Event(Clock.System.now(), null, DEFAULT_DURATION, 0, null), randomColor()))
            }) {
                Text("New Event")
            }

            Button({
                calendarObjects.add(0, CalendarTask(Task(DEFAULT_DURATION, mutableListOf(0), mutableListOf(), mutableListOf(), Clock.System.now(), null), randomColor()))
            }) {
                Text("New Task")
            }

            val last = calendarObjects.lastOrNull()

            if (last is ColorableCalendarObject) {
                ColorPicker(last.color) {
                    last.color = it
                    calendarObjects.forceUpdate()
                }
            }

            if (last is CalendarEvent) {
                val timeFun = DatetimePicker(last.event.time.toLocalDateTime(timeZone)) {
                    last.event.time = it.toInstant(timeZone)
                    calendarObjects.forceUpdate()
                }

                val durationFun = DurationPicker(last.event.duration) {
                    last.event.duration = it
                    calendarObjects.forceUpdate()
                }

                val repeatVal = if (last.event.repeat != null) {
                    last.event.repeat!!
                } else {
                    DEFAULT_REPEAT
                }

                val repeatFun = DurationPicker(repeatVal) {
                    last.event.repeat = it
                    calendarObjects.forceUpdate()
                }

                Switch(last.event.repeat != null, {
                    last.event.repeat = if (it) {
                        repeatFun(DEFAULT_REPEAT)
                        DEFAULT_REPEAT
                    } else {
                        null
                    }
                    calendarObjects.forceUpdate()
                })

                var addingTask by remember { mutableStateOf(false) }
                Switch(addingTask, {
                    addingTask = it
                })

                eventUpdateFun = {
                    if (it is CalendarEvent) {
                        if (it != last) {
                            addingTask = false
                        }

                        timeFun(it.event.time.toLocalDateTime(timeZone))
                        durationFun(it.event.duration)

                        if (it.event.repeat != null) {
                            repeatFun(it.event.repeat!!)
                        }
                    } else {
                        if (addingTask) {
                            if (it is CalendarTask) {
                                if (last.event.task == it.task) {
                                    last.event.task = null
                                    it.task.event = null
                                } else {
                                    last.event.task = it.task
                                    it.task.event = last.event
                                }
                            }
                        }

                        addingTask = false
                    }
                }
            }

            if (last is CalendarTask) {
                val timeFun = DatetimePicker(last.task.dueTime.toLocalDateTime(timeZone)) {
                    last.task.dueTime = it.toInstant(timeZone)
                    calendarObjects.forceUpdate()
                }

                val durationFun = DurationPicker(last.task.duration) {
                    last.task.duration = it
                    calendarObjects.forceUpdate()
                }

                var adding by remember { mutableStateOf(false) }
                Switch(adding, {
                    adding = it
                })

                taskUpdateFun = {
                    if (it is CalendarTask) {
                        if (it != last) {
                            if (last.task.requirements.contains(it.task)) {
                                last.task.requirements.remove(it.task)
                                it.task.requiredFor.remove(last.task)
                            } else if (last.task.requiredFor.contains(it.task)) {
                                last.task.requiredFor.remove(it.task)
                                it.task.requirements.remove(last.task)
                            } else {
                                last.task.requiredFor.add(it.task)
                                it.task.requirements.add(last.task)
                            }

                            adding = false
                        }

                        timeFun(it.task.dueTime.toLocalDateTime(timeZone))
                        durationFun(it.task.duration)
                    } else {
                        if (adding) {
                            if (it is CalendarEvent) {
                                if (last.task.event == it.event) {
                                    last.task.event = null
                                    it.event.task = null
                                } else {
                                    last.task.event = it.event
                                    it.event.task = last.task
                                }
                            }
                        }

                        adding = false
                    }
                }
            }
        }

        Column {
            val calendarTimeState = remember {
                val datetime = Clock.System.now().toLocalDateTime(timeZone)
                mutableStateOf(datetime.copy(hour = 0, minute = 0, second = 0, nanosecond = 0).toInstant(timeZone))
            }
            val calendarEventsGenerated = remember { mutableStateListOf<CalendarEvent>() }

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

                Button({
                    val eventsAndTasks = calendarObjectsToEventsAndTasks(calendarObjects)
                    val locationData = LocationData()
                    locationData.setTime(0, 0, 20.minutes)

                    for (calendarObject in calendarEventsGenerated) {
                        calendarObject.event.task?.event = null
                    }
                    calendarEventsGenerated.clear()

                    for (event in autofillMinTime(Clock.System.now(), eventsAndTasks.first, eventsAndTasks.second, locationData)) {
                        calendarEventsGenerated.add(CalendarEvent(event, randomColor()))
                    }
                }) {
                    Text("Auto Fill")
                }

                val calendarDatetime = calendarTime.toLocalDateTime(timeZone)
                Text("${calendarDatetime.month} ${calendarDatetime.dayOfMonth}, ${calendarDatetime.year}")
            }

            RenderedCalendar(calendarObjects, calendarEventsGenerated, calendarTimeState, timeZone, {
                eventUpdateFun(it)
                taskUpdateFun(it)
            }, Modifier.fillMaxSize())
        }
    }
}
