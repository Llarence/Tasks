package me.llarence.common

import kotlinx.datetime.Instant

// Events must be sorted by time
fun firstTimeAndLocation(task: Task, startTime: Instant, events: List<Event>, locationTimes: LocationTimes, endTime: Instant = task.dueTime): Pair<Instant, Int>? {
    var bestStartTime = startTime
    var bestEndTime = startTime + task.duration
    if (bestEndTime > endTime) {
        return null
    }

    var bestLocation = task.locations.first()

    for (event in events) {
        // Hate to call the same function twice, but it looks nicer
        val bestLocationForEvent = task.locations.minBy { locationTimes.get(it, event.location) }
        val bestLocationTimeTo = locationTimes.get(bestLocationForEvent, event.location)

        val bestStartForEvent = event.time + event.duration + bestLocationTimeTo
        if (event.time + event.duration + bestLocationTimeTo < bestStartTime) {
            break
        }

        val bestLocationTimeFrom = task.locations.minOf { locationTimes.get(event.location, it) }
        if (event.time > bestEndTime + bestLocationTimeFrom) {
            continue
        }

        bestStartTime = bestStartForEvent
        bestEndTime = bestStartForEvent + task.duration
        bestLocation = bestLocationForEvent

        if (bestEndTime > endTime) {
            return null
        }
    }

    return Pair(bestStartTime, bestLocation)
}

fun autofillMinTime(currTime: Instant, events: List<Event>, tasks: List<Task>, locationTimes: LocationTimes): List<Event> {
    val eventsByTime = events.sortedBy { it.time }
    val newEvents = mutableListOf<Event>()

    val tasksTodoByLength = tasks.filter { it.event == null }.toMutableList()
    tasksTodoByLength.shuffle()
    tasksTodoByLength.sortBy { it.dueTime }

    while (tasksTodoByLength.isNotEmpty()) {
        val allEvents = (eventsByTime + newEvents).sortedBy { it.time }

        var bestTimeAndLocation = Pair(Instant.DISTANT_FUTURE, 0)
        var bestTask: Task? = null
        var bestIndex = -1
        for (i in tasksTodoByLength.indices) {
            val task = tasksTodoByLength[i]
            // Having bestTimeAndLocation.first as the endTime doesn't cutoff it being the same
            val timeAndLocation = firstTimeAndLocation(task, currTime, allEvents, locationTimes, bestTimeAndLocation.first)
            if (timeAndLocation != null && timeAndLocation.first < bestTimeAndLocation.first) {
                bestTimeAndLocation = timeAndLocation
                bestTask = task
                bestIndex = i
            }
        }

        val event = Event(bestTimeAndLocation.first, null, bestTask!!.duration, bestTimeAndLocation.second, bestTask)
        bestTask.event = event
        newEvents.add(event)
        tasksTodoByLength.removeAt(bestIndex)
    }

    return newEvents
}
