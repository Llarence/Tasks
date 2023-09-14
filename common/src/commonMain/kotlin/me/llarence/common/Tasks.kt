package me.llarence.common

import kotlinx.datetime.Instant
import org.json.JSONArray
import org.json.JSONObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

// Maybe these could be data classes

class LocationData() {
    constructor(json: JSONObject) : this() {
        for (entryJson in json.getJSONArray("names").loop()) {
            entryJson as JSONObject
            names[entryJson.getInt("location")] = entryJson.getString("name")
        }

        for (entryJson in json.getJSONArray("durations").loop()) {
            entryJson as JSONObject
            setTime(entryJson.getInt("from"), entryJson.getInt("to"), entryJson.getLong("time").nanoseconds)
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

    fun toJson(): JSONObject {
        val json = JSONObject()

        val namesJson = JSONArray()
        for (entry in names.entries) {
            val entryJson = JSONObject()

            entryJson.put("location", entry.key)
            entryJson.put("name", entry.value)

            namesJson.put(entryJson)
        }

        json.put("names", namesJson)

        val durationsJson = JSONArray()
        for (entry in durations.entries) {
            val entryJson = JSONObject()

            entryJson.put("from", entry.key.first)
            entryJson.put("to", entry.key.second)
            entryJson.put("time", entry.value.inWholeNanoseconds)

            durationsJson.put(entryJson)
        }

        json.put("durations", durationsJson)

        return json
    }
}

// Json doesn't include task
// Earlier locations are prioritised
class Event(var time: Instant, var repeat: Duration?, var duration: Duration, var location: Int, var task: Task?) {
    constructor(jsonWithoutTask: JSONObject) : this(Instant.parse(jsonWithoutTask.getString("time")), (jsonWithoutTask.opt("repeat") as Long?)?.nanoseconds, jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getInt("location"), null)

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
    constructor(jsonWithoutTask: JSONObject) : this(jsonWithoutTask.getLong("duration").nanoseconds, jsonWithoutTask.getJSONArray("locations").loop().toMutableList() as MutableList<Int>, mutableListOf(), mutableListOf(), Instant.parse(jsonWithoutTask.getString("dueTime")), null)

    fun toJsonWithoutRequirementsAndEvent(): JSONObject {
        val json = JSONObject()

        json.put("duration", duration.inWholeNanoseconds)
        json.put("locations", JSONArray(locations))
        json.put("dueTime", dueTime.toString())

        return json
    }
}
