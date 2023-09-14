package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LocationPage(stateDelegate: MutableState<Int>, locationData: LocationData) {
    var state by stateDelegate

    // This is a hack to tell compose something has changed (state will tell the other pages)
    var refresh by remember { mutableStateOf(false) }

    Column {
        Button({
            state = MAIN_STATE
        }) {
            Text("Back", overflow = TextOverflow.Ellipsis, softWrap = false)
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
            Text("New", overflow = TextOverflow.Ellipsis, softWrap = false)
        }

        Row(Modifier.fillMaxSize()) {
            refresh

            Column {
                Text("Deletes", color = MaterialTheme.colors.contentColorFor(MaterialTheme.colors.background), overflow = TextOverflow.Ellipsis, softWrap = false)

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
                        Text("Delete", overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text("Names", color = MaterialTheme.colors.onBackground, overflow = TextOverflow.Ellipsis, softWrap = false)
                for (name in locationData.names.entries) {
                    TextField(name.value, {
                        locationData.names[name.key] = it
                        refresh = !refresh
                    }, textStyle = TextStyle(MaterialTheme.colors.onBackground), singleLine = true)
                }
            }

            for (from in locationData.names.entries) {
                Column(Modifier.weight(1f)) {
                    Text(from.value, color = MaterialTheme.colors.onBackground, overflow = TextOverflow.Ellipsis, softWrap = false)

                    for (to in locationData.names.entries) {
                        DurationPicker(locationData.getTime(from.key, to.key)) {
                            locationData.setTime(from.key, to.key, it)
                            refresh = !refresh
                        }
                    }
                }
            }
        }
    }
}