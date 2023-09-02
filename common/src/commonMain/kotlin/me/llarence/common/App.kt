package me.llarence.common

import androidx.compose.runtime.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds

// TODO: Look into using immutable classes or mutableStateOf in classes instead of hacking compose
// TODO: Make sure locationData is always updated when switching pages
// Maybe calendar does has a callback when setting a new CalendarObject as selected

val stateDelegate = mutableStateOf(0)

val timeZone = TimeZone.currentSystemDefault()

val calendarTimeState = mutableStateOf(Clock.System.now().toLocalDateTime(timeZone).copy(hour = 0, minute = 0, second = 0, nanosecond = 0).toInstant(timeZone))

val calendarObjects = mutableStateListOf<CalendarObject>()
val calendarEventsGenerated = mutableStateListOf<CalendarEvent>()

var locationData = LocationData().apply {
    names[0] = ""
    durations[Pair(0, 0)] = 0.nanoseconds
}

@Composable
fun app() {
    val state by stateDelegate

    init()

    when (state) {
        MAIN_STATE -> MainPage(stateDelegate, calendarObjects, calendarEventsGenerated, locationData, calendarTimeState, timeZone)
        LOCATION_STATE -> LocationPage (stateDelegate, locationData)
    }
}
