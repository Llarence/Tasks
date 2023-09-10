package me.llarence.common

import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

// TODO: Make some of these dp (maybe?)

const val HOURS_IN_DAY = 24
const val DAYS_IN_WEEK = 7

val HOUR_SIZE = 60.dp
val DAY_PADDING = 10.dp

val TEXT_PADDING = 2.dp

val CORNER_RADIUS = 10.dp

const val DARKEN_PERCENT = 0.8f

val DEFAULT_DURATION = 1.hours

// Maybe make duration
const val TASK_HOURS = 0.2f

val SCROLL_SPEED = 0.5.dp

const val START_HOUR = 8

// TODO: Rename
val PATH_STRENGTH = 100.dp
val PATH_INTERVALS = 10.dp

const val NANOS_PER_HOUR =  60f * 60f * 1000f * 1000f * 1000f
const val HOURS_PER_NANO = 1 / NANOS_PER_HOUR

const val MINUTES_PER_HOUR = 60

val DEFAULT_REPEAT = 1.days

const val MAIN_STATE = 0
const val LOCATION_STATE = 1

val SIDEPANEL_SIZE = 200.dp
