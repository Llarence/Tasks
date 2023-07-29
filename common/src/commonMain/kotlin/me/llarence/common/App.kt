package me.llarence.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.util.*

@Composable
fun app() {
    Calendar.Builder().build()
    val startDate = Calendar.getInstance()
    startDate.set(Calendar.DAY_OF_WEEK, 3)
    RenderedCalendar(remember { mutableStateListOf(Event(Time(startDate, 0.4f), 0.6f, 0)) }, Calendar.getInstance(), Modifier.fillMaxSize())
}
