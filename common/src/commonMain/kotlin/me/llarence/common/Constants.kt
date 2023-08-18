package me.llarence.common

import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.hours

// TODO: Make some of these dp (maybe?)

const val HOURS_IN_DAY = 24
const val DAYS_IN_WEEK = 7

const val HOUR_SIZE = 60f
const val DAY_PADDING = 10f

const val TEXT_PADDING = 2f

val CORNER_RADIUS = 10.dp

const val DARKEN_PERCENT = 0.8f

const val HOUR_SNAP = 0.2f

val DEFAULT_DURATION = 1.hours

const val TASK_HOURS = 0.2f

const val SCROLL_SPEED = 0.5f

const val START_HOUR = 8

// TODO: Rename
val PATH_STRENGTH = 100.dp
val PATH_INTERVALS = 10.dp

const val NANOS_IN_HOUR =  60f * 60f * 1000f * 1000f * 1000f
const val HOURS_IN_NANO = 1 / NANOS_IN_HOUR
