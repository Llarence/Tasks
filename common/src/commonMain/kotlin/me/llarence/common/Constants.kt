package me.llarence.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds

// TODO: Make some of these dp (maybe?)

const val HOURS_IN_DAY = 24
const val DAYS_IN_WEEK = 7

const val HOUR_SIZE = 60f
const val DAY_PADDING = 10f

const val TEXT_PADDING = 2f

const val CORNER_RADIUS = 10f

const val DARKEN_PERCENT = 0.8f

const val HOUR_SNAP = 0.2f

val DEFAULT_DURATION = 1.hours

const val TASK_HOURS = 0.2f

const val SCROLL_SPEED = 0.5f

// TODO: Rename
const val PATH_STRENGTH = 100
const val PATH_INTERVALS = 10f

const val NANOS_IN_HOUR =  60f * 60f * 1000f * 1000f * 1000f
const val HOURS_IN_NANO = 1 / NANOS_IN_HOUR

val Float.hours: Duration
    get() = (this * NANOS_IN_HOUR).toLong().nanoseconds
