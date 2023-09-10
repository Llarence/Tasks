package me.llarence.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.contentColorFor
import androidx.compose.material.darkColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    MaterialTheme(colors = darkColors(secondary = Color(0, 71, 138), onSecondary = Color.White)) {
        init()

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            val state by stateDelegate

            when (state) {
                MAIN_STATE -> MainPage(
                    stateDelegate,
                    calendarObjects,
                    calendarEventsGenerated,
                    locationData,
                    calendarTimeState,
                    timeZone
                )

                LOCATION_STATE -> LocationPage(stateDelegate, locationData)
            }
        }
    }
}
