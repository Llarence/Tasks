package me.llarence.common

import org.json.JSONArray
import org.json.JSONObject

fun toJson(tasks: List<Task>, events: List<Event>): JSONObject {
    val json = JSONObject()

    val tasksJson = JSONArray()
    for (task in tasks) {
        val taskJson = task.toJson()

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

    val eventsJson = JSONArray()
    for (event in events) {
        val eventJson = event.toJson()

        if (event.task != null) {
            eventJson.put("task", tasks.indexOf(event.task))
        }

        eventsJson.put(eventJson)
    }

    json.put("tasks", tasksJson)
    json.put("events", eventsJson)

    return json
}

fun fromJson(json: JSONObject): Pair<List<Task>, List<Event>> {
    val tasks = mutableListOf<Task>()
    val events = mutableListOf<Event>()

    val tasksJson = json.getJSONArray("tasks")
    for (taskJson in tasksJson) {
        tasks.add(Task(taskJson as JSONObject))
    }

    val eventsJson = json.getJSONArray("events")
    for (eventJson in eventsJson) {
        events.add(Event(eventJson as JSONObject))
    }

    for (i in tasks.indices) {
        val taskJson = tasksJson.getJSONObject(i)
        val task = tasks[i]

        for (requirementJson in taskJson.getJSONArray("requirements")) {
            task.requirements.add(tasks[requirementJson as Int])
        }

        for (requiredFor in taskJson.getJSONArray("requiredFor")) {
            task.requiredFor.add(tasks[requiredFor as Int])
        }

        val index = eventsJson.getJSONObject(i).opt("task")
        if (index is Int) {
            task.event = events[index]
        }
    }

    for (i in events.indices) {
        val index = eventsJson.getJSONObject(i).opt("event")
        if (index is Int) {
            events[i].task = tasks[index]
        }
    }

    return Pair(tasks, events)
}
