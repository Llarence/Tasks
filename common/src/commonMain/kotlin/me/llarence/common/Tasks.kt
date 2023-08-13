package me.llarence.common

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject
import java.sql.Time

// TODO: See if there is a better way than using instant everywhere then converting it

// Json doesn't include task
class Event(var time: Instant, var duration: Float, var location: Int, var task: Task?) {
    constructor(jsonWithoutTask: JSONObject) : this(Instant.parse(jsonWithoutTask.getString("time")), jsonWithoutTask.getFloat("duration"), jsonWithoutTask.getInt("location"), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("time", time.toString())
        json.put("duration", duration)
        json.put("location", location)

        return json
    }
}

// Json doesn't include requirements, requiredFor, or event
class Task(var duration: Float, val locations: MutableList<Int>, val requirements: MutableList<Task>, val requiredFor: MutableList<Task>, var dueTime: Instant, var event: Event?) {
    constructor(jsonWithoutTask: JSONObject) : this(jsonWithoutTask.getFloat("duration"), jsonWithoutTask.getJSONArray("locations").toMutableList() as MutableList<Int>, mutableListOf(), mutableListOf(), Instant.parse(jsonWithoutTask.getString("dueTime")), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("duration", duration)
        json.put("locations", JSONArray(locations))
        json.put("dueTime", dueTime.toString())

        return json
    }
}
