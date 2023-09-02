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
import org.json.JSONArray
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

val numberOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

// org.json in android is different than the import and JSONArray doesn't implement Iterator
fun JSONArray.loop(): Sequence<Any> {
    return sequence {
        for (i in 0 until this@loop.length()) {
            yield(this@loop.get(i))
        }
    }
}

fun randomColor(): Color {
    return Color(Random.nextBits(8), Random.nextBits(8), Random.nextBits(8))
}

fun LocalDateTime.copy(year: Int = this.year, monthNumber: Int = this.monthNumber, dayOfMonth: Int = this.dayOfMonth, hour: Int = this.hour, minute: Int = this.minute, second: Int = this.second, nanosecond: Int = this.nanosecond): LocalDateTime {
    return LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
}

@Composable
fun ColorPicker(color: Color, onChange: (Color) -> Unit) {
    Slider(color.red, {
        onChange(color.copy(red = it))
    }, colors = SliderDefaults.colors(thumbColor = Color(color.red, 0f, 0f)))

    Slider(color.green, {
        onChange(color.copy(green = it))
    }, colors = SliderDefaults.colors(thumbColor = Color(0f, color.green, 0f)))

    Slider(color.blue, {
        onChange(color.copy(blue = it))
    }, colors = SliderDefaults.colors(thumbColor = Color(0f, 0f, color.blue)))
}

@Composable
fun DatetimePicker(datetime: LocalDateTime, onChange: (LocalDateTime) -> Unit): (LocalDateTime) -> Unit {
    var year by remember { mutableStateOf(datetime.year.toString()) }
    var month by remember { mutableStateOf(datetime.monthNumber.toString()) }
    var day by remember { mutableStateOf(datetime.dayOfMonth.toString()) }
    var hour by remember { mutableStateOf(datetime.hour.toString()) }
    var minute by remember { mutableStateOf(datetime.minute.toString()) }

    val tryGenerateDatetime = {
        val yearInt = year.toIntOrNull()
        val monthInt = month.toIntOrNull()
        val dayInt = day.toIntOrNull()
        val hourInt = hour.toIntOrNull()
        val minuteInt = minute.toIntOrNull()

        if (yearInt != null && monthInt != null && dayInt != null && hourInt != null && minuteInt != null) {
            val date = try {
                // Just trust that this makes it always able to be converted to an Instant
                LocalDateTime(yearInt, monthInt, dayInt, hourInt, minuteInt)
            } catch (_: IllegalArgumentException) {
                null
            }

            if (date != null) {
                onChange(date)
            }
        }
    }

    Column {
        Row {
            TextField(day, {
                day = it
                tryGenerateDatetime()
            }, Modifier.width(50.dp), keyboardOptions = numberOptions)
            TextField(month, {
                month = it
                tryGenerateDatetime()
            }, Modifier.width(50.dp), keyboardOptions = numberOptions)
            TextField(year, {
                year = it
                tryGenerateDatetime()
            }, Modifier.width(100.dp), keyboardOptions = numberOptions)
        }

        Row {
            TextField(hour, {
                hour = it
                tryGenerateDatetime()
            }, Modifier.width(50.dp), keyboardOptions = numberOptions)
            TextField(minute, {
                minute = it
                tryGenerateDatetime()
            }, Modifier.width(50.dp), keyboardOptions = numberOptions)
        }
    }

    return {
        year = it.year.toString()
        month = it.monthNumber.toString()
        day = it.dayOfMonth.toString()
        hour = it.hour.toString()
        minute = it.minute.toString()
    }
}

@Composable
fun DurationPicker(duration: Duration, onChange: (Duration) -> Unit): (Duration) -> Unit {
    var hours by remember { mutableStateOf(duration.inWholeHours.toString()) }
    var minutes by remember { mutableStateOf((duration.inWholeMinutes - (duration.inWholeHours * MINUTES_PER_HOUR)).toString()) }

    val tryGenerateDatetime = {
        val hoursInt = hours.toIntOrNull()
        val minutesInt = minutes.toIntOrNull()

        if (hoursInt != null && minutesInt != null) {
            if ((hoursInt > 0 || minutesInt > 0) && minutesInt in 0 until MINUTES_PER_HOUR) {
                onChange(hoursInt.hours + minutesInt.minutes)
            }
        }
    }

    Row {
        TextField(hours, {
            hours = it
            tryGenerateDatetime()
        }, Modifier.width(50.dp), keyboardOptions = numberOptions)
        TextField(minutes, {
            minutes = it
            tryGenerateDatetime()
        }, Modifier.width(50.dp), keyboardOptions = numberOptions)
    }

    return {
        val hoursInt = it.inWholeHours
        hours = hoursInt.toString()
        minutes = (it.inWholeMinutes - (hoursInt * MINUTES_PER_HOUR)).toString()
    }
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
