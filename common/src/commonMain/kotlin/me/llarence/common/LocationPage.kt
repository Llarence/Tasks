package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*

@Composable
fun LocationPage(stateDelegate: MutableState<Int>, locationData: LocationData) {
    var state by stateDelegate

    // This is a hack to tell compose something has changed (state will tell the other pages)
    var refresh by remember { mutableStateOf(false) }

    Column {
        Button({
            state = MAIN_STATE
        }) {
            Text("Back")
        }

        Button({
            var newLocation = -1

            var notDone = true
            while (notDone) {
                notDone = false
                newLocation += 1

                for (location in locationData.names.keys) {
                    if (newLocation == location) {
                        notDone = true
                        break
                    }
                }
            }

            locationData.names[newLocation] = ""
            refresh = !refresh
        }) {
            Text("New")
        }

        Row {
            refresh

            Column {
                Text("Names")
                for (name in locationData.names.entries) {
                    TextField(name.value, {
                        locationData.names[name.key] = it
                        refresh = !refresh
                    })
                }
            }

            for (from in locationData.names.entries) {
                Column {
                    Text(from.value)

                    for (to in locationData.names.entries) {
                        DurationPicker(locationData.getTime(from.key, to.key)) {
                            locationData.setTime(from.key, to.key, it)
                            refresh = !refresh
                        }
                    }
                }
            }

            Column {
                Text("Deletes")

                val enabled = locationData.names.size > 1
                for (name in locationData.names.entries) {
                    Button({
                        locationData.names.remove(name.key)

                        for (otherName in locationData.names.entries) {
                            locationData.durations.remove(Pair(name.key, otherName.key))
                            locationData.durations.remove(Pair(otherName.key, name.key))
                        }

                        refresh = !refresh
                    }, enabled = enabled) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}