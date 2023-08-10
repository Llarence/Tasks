package me.llarence.common

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

// TODO: Use calendar to get hour
class Time(var date: Calendar, var hour: Float) {
    constructor(json: JSONObject) : this(Calendar.Builder().setInstant(json.getLong("epochSecond")).build(), json.getFloat("hour"))

    fun toJson(): JSONObject {
        val json = JSONObject()
        val instant = date.toInstant()

        json.put("epochSecond", instant.epochSecond)
        json.put("hour", hour)

        return json
    }
}

// Json doesn't include task
class Event(var time: Time, var duration: Float, var location: Int, var task: Task?) {
    constructor(jsonWithoutTask: JSONObject) : this(Time(jsonWithoutTask.getJSONObject("time")), jsonWithoutTask.getFloat("duration"), jsonWithoutTask.getInt("location"), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("time", time.toJson())
        json.put("duration", duration)
        json.put("location", location)

        return json
    }
}

// Json doesn't include requirements, requiredFor, or event
class Task(var duration: Float, val locations: MutableList<Int>, val requirements: MutableList<Task>, val requiredFor: MutableList<Task>, var dueTime: Time, var event: Event?) {
    constructor(jsonWithoutTask: JSONObject) : this(jsonWithoutTask.getFloat("duration"), jsonWithoutTask.getJSONArray("locations").toMutableList() as MutableList<Int>, mutableListOf(), mutableListOf(), Time(jsonWithoutTask.getJSONObject("dueTime")), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("duration", duration)
        json.put("locations", JSONArray(locations))
        json.put("dueTime", dueTime.toJson())

        return json
    }
}
