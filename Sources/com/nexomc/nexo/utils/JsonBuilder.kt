package com.nexomc.nexo.utils

import com.google.gson.JsonNull
import com.google.gson.JsonParser
import team.unnamed.creative.base.Writable

typealias JsonObject = com.google.gson.JsonObject
typealias JsonArray = com.google.gson.JsonArray
typealias JsonPrimitive = com.google.gson.JsonPrimitive
typealias JsonElement = com.google.gson.JsonElement

fun Writable.toJsonObject(): JsonObject? = runCatching { JsonParser.parseString(toUTF8String()).asJsonObject }.getOrNull()
fun Writable.toJsonArray(): JsonArray? = runCatching { JsonParser.parseString(toUTF8String()).asJsonArray }.getOrNull()
fun JsonObject.toWritable(): Writable = Writable.stringUtf8(this.toString())

object JsonBuilder {

    val jsonObject get(): JsonObject = JsonObject()
    val jsonArray get(): JsonArray = JsonArray()

    fun JsonObject.`object`(key: String): JsonObject? = runCatching { this.getAsJsonObject(key) }.getOrNull()
    fun JsonObject.primitive(key: String): JsonPrimitive? = runCatching { this.getAsJsonPrimitive(key) }.getOrNull()
    fun JsonObject.array(key: String): JsonArray? = runCatching { this.getAsJsonArray(key) }.getOrNull()
    fun JsonArray.objects(): List<JsonObject> = this.asJsonArray.asList().filterIsInstance<JsonObject>()

    fun JsonObject.minus(string: String): JsonObject {
        this.remove(string)
        return this
    }

    fun JsonObject.plus(string: String, any: Any?) = apply {
        when (any) {
            is Boolean -> addProperty(string, any)
            is Number -> addProperty(string, any)
            is String -> addProperty(string, any)
            is Char -> addProperty(string, any)
            is JsonElement -> add(string, any)
        }
    }

    fun List<JsonElement>.toJsonArray(): JsonArray = JsonArray().apply {
        this@toJsonArray.forEach {
            add(it)
        }
    }

    fun JsonArray.plus(any: Any?): JsonArray = apply {
        when (any) {
            null -> add(JsonNull.INSTANCE)
            is Boolean -> add(any)
            is Number -> add(any)
            is String -> add(any)
            is Char -> add(any)
            is JsonArray -> addAll(any)
            is JsonElement -> add(any)
            is Collection<*> -> any.forEach { element: Any? -> this.plus(element) }
        }
    }
}