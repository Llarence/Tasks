package me.llarence.common

import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject

// TODO: See if there is a better way than using instant everywhere then converting it

class LocationTimes {
    constructor(json: JSONArray) {
        for (entryJson in json) {
            entryJson as JSONObject
            set(entryJson.getInt("From"), entryJson.getInt("To"), entryJson.getFloat("Distance"))
        }
    }

    val map = mutableMapOf<Pair<Int, Int>, Float>()

    fun set(from: Int, to: Int, value: Float) {
        map[Pair(from, to)] = value
    }

    fun get(from: Int, to: Int): Float {
        return map[Pair(from, to)]!!
    }

    fun toJson(): JSONArray {
        val json = JSONArray()

        for (entry in map.entries) {
            val entryJson = JSONObject()

            entryJson.put("From", entry.key.first)
            entryJson.put("To", entry.key.second)
            entryJson.put("Distance", entry.value)

            json.put(entryJson)
        }

        return json
    }
}

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
