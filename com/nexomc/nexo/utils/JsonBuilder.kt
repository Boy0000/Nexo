package com.nexomc.nexo.utils

typealias JsonObject = com.google.gson.JsonObject
typealias JsonArray = com.google.gson.JsonArray
typealias JsonPrimitive = com.google.gson.JsonPrimitive
typealias JsonElement = com.google.gson.JsonElement

object JsonBuilder {

    val jsonObject get(): JsonObject = JsonObject()
    val jsonArray get(): JsonArray = JsonArray()

    fun JsonObject.`object`(key: String): JsonObject? = this.getAsJsonObject(key)
    fun JsonObject.primitive(key: String): JsonPrimitive? = this.getAsJsonPrimitive(key)
    fun JsonObject.array(key: String): JsonArray? = this.getAsJsonArray(key)
    fun JsonArray.objects(): List<JsonObject> = this.asJsonArray.asList().filterIsInstance<JsonObject>()

    fun JsonObject.plus(string: String, any: Any) = apply {
        when (any) {
            is Boolean -> addProperty(string, any)
            is Number -> addProperty(string, any)
            is String -> addProperty(string, any)
            is Char -> addProperty(string, any)
            is JsonElement -> add(string, any)
        }
    }
    fun List<JsonObject>.toJsonArray(): JsonArray = JsonArray().apply {
        this@toJsonArray.forEach {
            add(it)
        }
    }
    fun JsonArray.plus(any: Any) = apply {
        when (any) {
            is Boolean -> add(any)
            is Number -> add(any)
            is String -> add(any)
            is Char -> add(any)
            is JsonArray -> addAll(any)
            is JsonElement -> add(any)
            is Collection<*> -> any.forEach { this@plus.plus(it) }
        }
    }
}