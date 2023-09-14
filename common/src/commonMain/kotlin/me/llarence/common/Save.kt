package me.llarence.common

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

val charset: Charset = Charset.defaultCharset()

fun save(file: File) {
    val json = JSONObject()
    json.put("calendar", toCalendarObjectsJson(calendarObjects + calendarEventsGenerated))
    json.put("location", locationData.toJson())

    file.writeBytes(json.toString().toByteArray(charset))
}

fun load(file: File) {
    println(file.readBytes().toString(charset))
    val json = JSONObject(file.readBytes().toString(charset))

    calendarObjects.clear()
    calendarEventsGenerated.clear()

    for (calendarObject in fromCalendarObjectsJson(json.getJSONObject("calendar"))) {
        if (calendarObject is CalendarEvent) {
            if (calendarObject.generated) {
                calendarEventsGenerated.add(calendarObject)
            }
        }

        calendarObjects.add(calendarObject)
    }

    locationData = LocationData(json.getJSONObject("location"))
}

fun toCalendarObjectsJson(calendarObjects: List<CalendarObject>): JSONObject {
    val events = mutableListOf<Event>()
    val tasks = mutableListOf<Task>()

    val calendarObjectsJson = JSONArray()

    for (calendarObject in calendarObjects) {
        val calendarObjectJson = calendarObject.toJson()

        if (calendarObject is CalendarEvent) {
            calendarObjectJson.put("event", events.size)
            events.add(calendarObject.event)
        } else if (calendarObject is CalendarTask) {
            calendarObjectJson.put("task", tasks.size)
            tasks.add(calendarObject.task)
        } else {
            throw IllegalArgumentException()
        }

        calendarObjectsJson.put(calendarObjectJson)
    }

    val json = toEventsAndTasksJson(events, tasks)
    json.put("calendarObjects", calendarObjectsJson)

    return json
}

fun fromCalendarObjectsJson(json: JSONObject): List<CalendarObject> {
    val calendarObjects = mutableListOf<CalendarObject>()

    val out = fromEventsAndTasksJson(json)
    val events = out.first
    val tasks = out.second

    for (calendarObjectJson in json.getJSONArray("calendarObjects").loop()) {
        calendarObjectJson as JSONObject

        val eventIndex = calendarObjectJson.opt("event")
        if (eventIndex is Int) {
            calendarObjects.add(CalendarEvent(events[eventIndex], calendarObjectJson))
            continue
        }

        val taskIndex = calendarObjectJson.opt("task")
        if (taskIndex is Int) {
            calendarObjects.add(CalendarTask(tasks[taskIndex], calendarObjectJson))
            continue
        }

        throw IllegalArgumentException()
    }

    return calendarObjects
}

fun toEventsAndTasksJson(events: List<Event>, tasks: List<Task>): JSONObject {
    val json = JSONObject()

    val eventsJson = JSONArray()
    for (event in events) {
        val eventJson = event.toJsonWithoutTask()

        if (event.task != null) {
            eventJson.put("task", tasks.indexOf(event.task))
        }

        eventsJson.put(eventJson)
    }

    val tasksJson = JSONArray()
    for (task in tasks) {
        val taskJson = task.toJsonWithoutRequirementsAndEvent()

        val requirementsJson = JSONArray()
        for (requirement in task.requirements) {
            requirementsJson.put(tasks.indexOf(requirement))
        }
        taskJson.put("requirements", requirementsJson)

        val requiredForJson = JSONArray()
        for (requiredFor in task.requiredFor) {
            requiredForJson.put(tasks.indexOf(requiredFor))
        }
        taskJson.put("requiredFor", requiredForJson)

        if (task.event != null) {
            taskJson.put("event", events.indexOf(task.event))
        }

        tasksJson.put(taskJson)
    }

    json.put("events", eventsJson)
    json.put("tasks", tasksJson)

    return json
}

fun fromEventsAndTasksJson(json: JSONObject): Pair<List<Event>, List<Task>> {
    val events = mutableListOf<Event>()
    val tasks = mutableListOf<Task>()

    val eventsJson = json.getJSONArray("events")
    for (eventJson in eventsJson.loop()) {
        events.add(Event(eventJson as JSONObject))
    }

    val tasksJson = json.getJSONArray("tasks")
    for (taskJson in tasksJson.loop()) {
        tasks.add(Task(taskJson as JSONObject))
    }

    for (i in events.indices) {
        val index = eventsJson.getJSONObject(i).opt("event")
        if (index is Int) {
            events[i].task = tasks[index]
        }
    }

    for (i in tasks.indices) {
        val taskJson = tasksJson.getJSONObject(i)
        val task = tasks[i]

        for (requirementJson in taskJson.getJSONArray("requirements").loop()) {
            task.requirements.add(tasks[requirementJson as Int])
        }

        for (requiredFor in taskJson.getJSONArray("requiredFor").loop()) {
            task.requiredFor.add(tasks[requiredFor as Int])
        }

        val index = eventsJson.getJSONObject(i).opt("task")
        if (index is Int) {
            task.event = events[index]
        }
    }

    return Pair(events, tasks)
}
