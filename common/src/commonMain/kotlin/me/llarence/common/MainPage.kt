package me.llarence.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import java.io.IOException
import kotlin.time.Duration.Companion.days

// TODO: Check to make sure that everything works with random recomposes?
@Composable
fun MainPage(stateDelegate: MutableState<Int>, calendarObjects: SnapshotStateList<CalendarObject>, calendarEventsGenerated: SnapshotStateList<CalendarEvent>, locationData: LocationData, calendarTimeState: MutableState<Instant>, timeZone: TimeZone) {
    var state by stateDelegate

    var eventUpdateFun by remember { mutableStateOf<(CalendarObject) -> Unit>({  }) }
    var taskUpdateFun by remember { mutableStateOf<(CalendarObject) -> Unit>({  }) }

    Row {
        Column(Modifier.width(SIDEPANEL_SIZE)) {
            Button({
                if (locationData.names.isNotEmpty()) {
                    calendarObjects.add(0, CalendarEvent(Event(Clock.System.now(), null, DEFAULT_DURATION, locationData.names.keys.random(), null), false, "", "", randomColor()))
                }
            }) {
                Text("New Event")
            }

            Button({
                calendarObjects.add(0, CalendarTask(Task(DEFAULT_DURATION, mutableListOf(locationData.names.keys.random()), mutableListOf(), mutableListOf(), Clock.System.now(), null), "", "", randomColor()))
            }) {
                Text("New Task")
            }

            Button({
                state = LOCATION_STATE
            }) {
                Text("Pick Locations")
            }

            val last = calendarObjects.lastOrNull()

            if (last is MetaDataCalendarObject) {
                TextField(last.title, {
                    last.title = it
                    calendarObjects.forceUpdateList()
                }, textStyle = TextStyle(MaterialTheme.colors.onBackground), singleLine = true)

                TextField(last.description, {
                    last.description = it
                    calendarObjects.forceUpdateList()
                }, textStyle = TextStyle(MaterialTheme.colors.onBackground))

                ColorPicker(last.color) {
                    last.color = it
                    calendarObjects.forceUpdateList()
                }
            }

            if (last is CalendarEvent) {
                val timeFun = DatetimePicker(last.event.time.toLocalDateTime(timeZone)) {
                    last.event.time = it.toInstant(timeZone)
                    calendarObjects.forceUpdateList()
                }

                val durationFun = DurationPicker(last.event.duration) {
                    last.event.duration = it
                    calendarObjects.forceUpdateList()
                }

                val repeatVal = if (last.event.repeat != null) {
                    last.event.repeat!!
                } else {
                    DEFAULT_REPEAT
                }

                val repeatFun = DurationPicker(repeatVal) {
                    last.event.repeat = it
                    calendarObjects.forceUpdateList()
                }

                Switch(last.event.repeat != null, {
                    last.event.repeat = if (it) {
                        repeatFun(DEFAULT_REPEAT)
                        DEFAULT_REPEAT
                    } else {
                        null
                    }
                    calendarObjects.forceUpdateList()
                })

                var addingTask by remember { mutableStateOf(false) }
                Switch(addingTask, {
                    addingTask = it
                })

                var expanded by remember { mutableStateOf(false) }

                Button({ expanded = !expanded }) {
                    Text(locationData.names[last.event.location]!!)
                }

                DropdownMenu(expanded, { expanded = false }) {
                    for (name in locationData.names) {
                        if (last.event.location != name.key) {
                            Button({
                                last.event.location = name.key
                                calendarObjects.forceUpdateList()

                                expanded = false
                            }) {
                                Text(name.value)
                            }
                        }
                    }
                }

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
                    calendarObjects.forceUpdateList()
                }

                val durationFun = DurationPicker(last.task.duration) {
                    last.task.duration = it
                    calendarObjects.forceUpdateList()
                }

                var adding by remember { mutableStateOf(false) }
                Switch(adding, {
                    adding = it
                })

                var expanded by remember { mutableStateOf(false) }

                Column {
                    val enabled = last.task.locations.size > 1
                    for (i in last.task.locations.indices) {
                        val location = last.task.locations[i]
                        Row {
                            Text(locationData.names[location]!!)
                            Button({
                                last.task.locations.removeAt(i)
                                calendarObjects.forceUpdateList()
                            }, enabled = enabled) {
                                Text("Remove")
                            }
                        }
                    }
                }

                Button({ expanded = !expanded }) {
                    Text("Add Location")
                }

                DropdownMenu(expanded, { expanded = false }) {
                    for (name in locationData.names) {
                        if (name.key !in last.task.locations) {
                            Button({
                                last.task.locations.add(name.key)
                                calendarObjects.forceUpdateList()

                                expanded = false
                            }) {
                                Text(name.value)
                            }
                        }
                    }
                }

                taskUpdateFun = {
                    if (it is CalendarTask) {
                        if (it != last && adding) {
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

            if (last != null) {
                Button({
                    calendarObjects.remove(last)

                    if (last is CalendarEvent) {
                        last.event.task!!.event = null
                    } else if (last is CalendarTask) {
                        last.task.event!!.task = null

                        for (other in last.task.requiredFor) {
                            other.requirements.remove(last.task)
                        }

                        for (other in last.task.requirements) {
                            other.requiredFor.remove(last.task)
                        }
                    }

                    calendarObjects.forceUpdateList()
                }) {
                    Text("Delete")
                }
            }
        }

        Column {
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

                var filename by remember { mutableStateOf("") }
                TextField(filename, {
                    filename = it
                }, textStyle = TextStyle(MaterialTheme.colors.onBackground))

                Button({
                    val file = try {
                        getFile("$filename.json")
                    } catch (_: IOException) {
                        null
                    }

                    if (file != null) {
                        save(file)
                    }
                }) {
                    Text("Save")
                }

                Button({
                    val file = try {
                        getFile("$filename.json")
                    } catch (_: IOException) {
                        null
                    }

                    if (file != null) {
                        load(file)
                    }
                }) {
                    Text("Load")
                }

                Button({
                    val eventsAndTasks = calendarObjectsToEventsAndTasks(calendarObjects)

                    for (calendarObject in calendarEventsGenerated) {
                        calendarObject.event.task?.event = null
                    }
                    calendarEventsGenerated.clear()

                    for (event in autofillMinTime(Clock.System.now(), eventsAndTasks.first, eventsAndTasks.second, locationData)) {
                        calendarEventsGenerated.add(CalendarEvent(event, true, "", "", randomColor()))
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
