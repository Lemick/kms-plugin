package com.lemick.kmstools.model

class JsonObject {
    private val map: MutableMap<String, String> = mutableMapOf()

    fun put(key: String, value: String) {
        map[key] = value
    }

    fun get(key: String): String {
        if (map.containsKey(key)) {
            return map[key]!!
        }

        throw IllegalArgumentException("Key $key does not exist")
    }

    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":\"$value\""
        }
    }

    companion object {
        fun parse(jsonString: String): JsonObject {
            val jsonObject = JsonObject()
            try {
                val entries = jsonString
                    .trim()
                    .trimStart('{').trimEnd('}')
                    .split(",")
                    .map { it.split(":") }
                    .map { it[0].trim().trim('"') to it[1].trim().trim('"') }

                for ((key, value) in entries) {
                    jsonObject.put(key, value)
                }
            } catch (e: Exception) {
                throw JsonParseException("Invalid JSON string")
            }

            return jsonObject
        }

        class JsonParseException(message: String) : Exception(message)
    }
}
