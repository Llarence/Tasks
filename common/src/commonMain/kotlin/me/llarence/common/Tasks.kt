package me.llarence.common

import java.util.*

class Time(var date: Calendar, var hour: Float)

class Event(var time: Time, var duration: Float, var location: Int, var task: Task?)

class Task(var duration: Float, val locations: MutableList<Int>, val requirements: MutableList<Task>, val requiredFor: MutableList<Task>, var dueTime: Time, var event: Event?)
