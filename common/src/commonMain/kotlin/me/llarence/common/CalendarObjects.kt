package me.llarence.common

import androidx.compose.ui.graphics.Color

// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage

abstract class CalendarObject(val color: Color, val corners: Float) {
    abstract val time: Time
    abstract val duration: Float

    abstract fun createWithNewColor(newColor: Color): CalendarObject

    // Requires the old object is no longer used because it doesn't copy some non primitives
    abstract fun createWithNewTime(newTime: Time): CalendarObject
}

class CalendarEvent(val event: Event, color: Color) : CalendarObject(color, CORNER_RADIUS) {
    override val time = event.time
    override val duration = event.duration

    override fun createWithNewColor(newColor: Color): CalendarObject {
        return CalendarEvent(event, newColor)
    }

    override fun createWithNewTime(newTime: Time): CalendarEvent {
        return CalendarEvent(Event(newTime, event.duration, event.location), color)
    }

    fun createWithDuration(newDuration: Float): CalendarEvent {
        return CalendarEvent(Event(event.time, newDuration, event.location), color)
    }
}

class CalendarTask(val task: Task, color: Color) : CalendarObject(color, 0f) {
    override val time = task.dueTime
    override val duration = TASK_HOURS

    override fun createWithNewColor(newColor: Color): CalendarObject {
        return CalendarTask(task, newColor)
    }

    override fun createWithNewTime(newTime: Time): CalendarTask {
        return CalendarTask(Task(task.duration, task.locations, task.requirements, newTime, task.event), color)
    }
}
