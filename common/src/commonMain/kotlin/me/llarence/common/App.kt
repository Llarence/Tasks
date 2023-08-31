package me.llarence.common

import androidx.compose.runtime.*

// TODO: Look into using immutable classes or mutableStateOf in classes instead of hacking compose
// TODO: Make sure locationData is always updated when switching pages
// Maybe calendar does has a callback when setting a new CalendarObject as selected
@Composable
fun app() {
    val stateDelegate = remember { mutableStateOf(0) }
    val state by stateDelegate

    val locationData by remember {
        val locationData = LocationData()
        locationData.names[0] = "a"
        locationData.names[8] = "a1"
        locationData.names[-6] = "ab"
        locationData.names[2] = "va"
        locationData.names[43] = "daa"
        locationData.names[6] = "dsa"
        locationData.names[4] = "sdea"
        locationData.names[3] = "aaa"
        mutableStateOf(locationData)
    }

    when (state) {
        MAIN_STATE -> MainPage(stateDelegate, locationData)
        LOCATION_STATE -> LocationPage(stateDelegate, locationData)
    }
}
