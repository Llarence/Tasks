package me.llarence.common

import androidx.compose.ui.graphics.Color

// TODO: Look into improving createWithNew functions by not making these object immutable to decrease ram usage

abstract class CalendarObject(var color: Color, val corners: Float) {
    abstract fun getTime(): Time
    abstract fun getDuration(): Float
}

class CalendarEvent(val event: Event, color: Color) : CalendarObject(color, CORNER_RADIUS) {
    override fun getTime(): Time {
        return event.time
    }

    override fun getDuration(): Float {
        return event.duration
    }
}

class CalendarTask(val task: Task, color: Color) : CalendarObject(color, 0f) {
    override fun getTime(): Time {
        return task.dueTime
    }

    override fun getDuration(): Float {
        return TASK_HOURS
    }
}

class CalendarDummy : CalendarObject(Color.Black, 0f) {
    override fun getTime(): Time {
        throw NotImplementedError()
    }

    override fun getDuration(): Float {
        throw NotImplementedError()
    }
}
