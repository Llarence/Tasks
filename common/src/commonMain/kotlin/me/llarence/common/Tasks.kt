package me.llarence.common

import java.util.*

class Time(val date: Calendar, val hour: Float)

class Event(val time: Time, val duration: Float, val location: Int)

class Task(val duration: Float, val locations: List<Int>, val requirements: List<Task>, val dueTime: Time)
