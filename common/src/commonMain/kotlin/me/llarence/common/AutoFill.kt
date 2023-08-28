package me.llarence.common

import kotlinx.datetime.Instant
import kotlin.math.floor

// This all could be optimized

fun repeatInstanceRightBefore(startTime: Instant, repeatedEvent: Event): Event {
    val repeats = floor((startTime - repeatedEvent.time) / repeatedEvent.repeat!!)

    return Event(repeatedEvent.time + (repeatedEvent.repeat!! * repeats), null, repeatedEvent.duration, repeatedEvent.location, null)
}

// Maybe break into functions
fun generateEvents(startTime: Instant, eventsByTime: List<Event>, repeatedEvents: List<Event>): Sequence<Event> {
    if (repeatedEvents.isEmpty()) {
        return eventsByTime.asSequence()
    }

    val repeatInstanceAndDurations = repeatedEvents.map { Pair(repeatInstanceRightBefore(startTime, it), it.repeat!!) }

    return sequence {
        for (event in eventsByTime) {
            while (true) {
                val earliest = repeatInstanceAndDurations.minBy { it.first.time }

                if (earliest.first.time <= event.time) {
                    yield(earliest.first.copyWithoutTask())
                    earliest.first.time += earliest.second
                } else {
                    break
                }
            }

            yield(event)
        }

        while (true) {
            val earliest = repeatInstanceAndDurations.minBy { it.first.time }

            yield(earliest.first.copyWithoutTask())
            earliest.first.time += earliest.second
        }
    }
}

fun firstTimeAndLocation(task: Task, startTime: Instant, eventsByTime: List<Event>, repeatedEvents: List<Event>, locationTimes: LocationTimes, endTime: Instant = task.dueTime): Pair<Instant, Int>? {
    var bestStartTime = startTime
    var bestEndTime = startTime + task.duration
    if (bestEndTime > endTime) {
        return null
    }

    var bestLocation = task.locations.first()

    for (event in generateEvents(startTime, eventsByTime, repeatedEvents)) {
        // Hate to call the same function twice, but it looks nicer
        val bestLocationForEvent = task.locations.minBy { locationTimes.get(it, event.location) }
        val bestLocationTimeTo = locationTimes.get(bestLocationForEvent, event.location)

        val bestStartForEvent = event.time + event.duration + bestLocationTimeTo
        if (bestStartForEvent < bestStartTime) {
            continue
        }

        val bestLocationTimeFrom = task.locations.minOf { locationTimes.get(event.location, it) }
        if (event.time > bestEndTime + bestLocationTimeFrom) {
            break
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
    val eventsByTime = events.filter { it.repeat == null }.sortedBy { it.time }
    val repeatedEvents = events.filter { it.repeat != null }
    val newEvents = mutableListOf<Event>()

    val tasksTodoByLength = tasks.filter { it.event == null }.toMutableList()
    tasksTodoByLength.shuffle()
    tasksTodoByLength.sortBy { it.dueTime }

    while (tasksTodoByLength.isNotEmpty()) {
        val allEventsByTime = (eventsByTime + newEvents).sortedBy { it.time }

        var bestTimeAndLocation = Pair(Instant.DISTANT_FUTURE, 0)
        var bestTask: Task? = null
        var bestIndex = -1
        for (i in tasksTodoByLength.indices) {
            val task = tasksTodoByLength[i]
            // Having bestTimeAndLocation.first as the endTime doesn't cutoff it being the same
            val timeAndLocation = firstTimeAndLocation(task, currTime, allEventsByTime, repeatedEvents, locationTimes, bestTimeAndLocation.first)
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
