package me.llarence.common

import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

// Maybe these could be data classes

class LocationData() {
    constructor(json: JSONArray) : this() {
        for (entryJson in json.get(0) as JSONArray) {
            entryJson as JSONObject
            names[entryJson.getInt("Location")] = entryJson.getString("Name")
        }

        for (entryJson in json.get(1) as JSONArray) {
            entryJson as JSONObject
            setTime(entryJson.getInt("From"), entryJson.getInt("To"), entryJson.getLong("Time").nanoseconds)
        }
    }

    val names = mutableMapOf<Int, String>()
    val durations = mutableMapOf<Pair<Int, Int>, Duration>()
    
    fun setTime(from: Int, to: Int, value: Duration) {
        durations[Pair(from, to)] = value
    }

    fun getTime(from: Int, to: Int): Duration {
        return durations.getOrDefault(Pair(from, to), 0.nanoseconds)
    }

    fun toJson(): JSONArray {
        val json = JSONArray()

        val namesJson = JSONArray()
        for (entry in names.entries) {
            val entryJson = JSONObject()

            entryJson.put("Location", entry.key)
            entryJson.put("Name", entry.value)

            namesJson.put(entryJson)
        }

        json.put(namesJson)

        val durationsJson = JSONArray()
        for (entry in durations.entries) {
            val entryJson = JSONObject()

            entryJson.put("From", entry.key.first)
            entryJson.put("To", entry.key.second)
            entryJson.put("Time", entry.value.inWholeNanoseconds)

            durationsJson.put(entryJson)
        }

        json.put(durationsJson)

        return json
    }
}

// Json doesn't include task
// Earlier locations are prioritised
class Event(var time: Instant, var repeat: Duration?, var duration: Duration, var location: Int, var task: Task?) {
    constructor(jsonWithoutTask: JSONObject) : this(Instant.parse(jsonWithoutTask.getString("time")), (jsonWithoutTask.get("repeat") as Long?)?.nanoseconds, jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getInt("location"), null)

    fun toJsonWithoutTask(): JSONObject {
        val json = JSONObject()

        json.put("time", time.toString())
        if (repeat != null) {
            json.put("repeat", repeat!!.inWholeNanoseconds)
        }
        json.put("duration", duration.inWholeNanoseconds)
        json.put("location", location)

        return json
    }

    fun copyWithoutTask(): Event {
        return Event(time, repeat, duration, location, null)
    }
}

// Json doesn't include requirements, requiredFor, or event
// event's repeat value should be null
class Task(var duration: Duration, val locations: MutableList<Int>, val requirements: MutableList<Task>, val requiredFor: MutableList<Task>, var dueTime: Instant, var event: Event?) {
    constructor(jsonWithoutTask: JSONObject) : this(jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getJSONArray("locations").toMutableList() as MutableList<Int>, mutableListOf(), mutableListOf(), Instant.parse(jsonWithoutTask.getString("dueTime")), null)

    fun toJsonWithoutRequirementsAndEvent(): JSONObject {
        val json = JSONObject()

        json.put("duration", duration.inWholeNanoseconds)
        json.put("locations", JSONArray(locations))
        json.put("dueTime", dueTime.toString())

        return json
    }
}
