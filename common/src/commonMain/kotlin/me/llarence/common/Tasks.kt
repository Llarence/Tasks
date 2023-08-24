package me.llarence.common

import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class LocationTimes() {
    constructor(json: JSONArray) : this() {
        for (entryJson in json) {
            entryJson as JSONObject
            set(entryJson.getInt("From"), entryJson.getInt("To"), entryJson.getLong("Time").nanoseconds)
        }
    }

    val map = mutableMapOf<Pair<Int, Int>, Duration>()

    fun set(from: Int, to: Int, value: Duration) {
        map[Pair(from, to)] = value
    }

    fun get(from: Int, to: Int): Duration {
        return map[Pair(from, to)]!!
    }

    fun toJson(): JSONArray {
        val json = JSONArray()

        for (entry in map.entries) {
            val entryJson = JSONObject()

            entryJson.put("From", entry.key.first)
            entryJson.put("To", entry.key.second)
            entryJson.put("Time", entry.value.inWholeNanoseconds)

            json.put(entryJson)
        }

        return json
    }
}

// Json doesn't include task
// Earlier locations are prioritised
class Event(var time: Instant, var repeat: Duration?, var duration: Duration, var location: Int, var task: Task?) {
    constructor(jsonWithoutTask: JSONObject) : this(Instant.parse(jsonWithoutTask.getString("time")), (jsonWithoutTask.get("repeat") as Long?)?.nanoseconds, jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getInt("location"), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("time", time.toString())
        if (repeat != null) {
            json.put("repeat", repeat!!.inWholeNanoseconds)
        }
        json.put("duration", duration.inWholeNanoseconds)
        json.put("location", location)

        return json
    }
}

// Json doesn't include requirements, requiredFor, or event
// event's repeat value should be null
class Task(var duration: Duration, val locations: MutableList<Int>, val requirements: MutableList<Task>, val requiredFor: MutableList<Task>, var dueTime: Instant, var event: Event?) {
    constructor(jsonWithoutTask: JSONObject) : this(jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getJSONArray("locations").toMutableList() as MutableList<Int>, mutableListOf(), mutableListOf(), Instant.parse(jsonWithoutTask.getString("dueTime")), null)

    fun toJson(): JSONObject {
        val json = JSONObject()

        json.put("duration", duration.inWholeNanoseconds)
        json.put("locations", JSONArray(locations))
        json.put("dueTime", dueTime.toString())

        return json
    }
}
