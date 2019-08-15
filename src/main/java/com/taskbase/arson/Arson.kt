package com.taskbase.arson

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.taskbase.reflection.KotlinReflectionUtils
import java.io.Reader
import java.lang.reflect.Type

/**
 * Wrapper for the Json serialization library Gson that performs additional fixes and checks on the deserialized objects.
 */
class Arson(
    private val gson: Gson,
    private val enhancers: List<(o: Any?) -> Any?> = listOf(KotlinReflectionUtils::addDefaultValues),
    private val checks: List<(T: Any?) -> Boolean> = listOf(KotlinReflectionUtils::containsIllegalNullValues)
) {
    fun <T> fromJson(json: String, classOfT: Class<T>): T = check(enhance(gson.fromJson<T>(json, classOfT)))
    fun <T> fromJson(json: String, typeOfT: Type): T = check(enhance(gson.fromJson(json, typeOfT)))
    fun <T> fromJson(json: Reader, classOfT: Class<T>): T = check(enhance(gson.fromJson(json, classOfT)))
    fun <T> fromJson(json: Reader, typeOfT: Type): T = check(enhance(gson.fromJson(json, typeOfT)))
    fun <T> fromJson(reader: JsonReader, typeOfT: Type): T = check(enhance(gson.fromJson(reader, typeOfT)))
    fun <T> fromJson(json: JsonElement, classOfT: Class<T>): T = check(enhance(gson.fromJson(json, classOfT)))
    fun <T> fromJson(json: JsonElement, typeOfT: Type): T = check(enhance(gson.fromJson(json, typeOfT)))

    fun toJson(src: Any?): String = gson.toJson(src)
    fun toJson(src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)
    fun toJson(src: Any?, writer: Appendable) = gson.toJson(src, writer)
    fun toJson(src: Any?, typeOfSrc: Type, writer: Appendable) = gson.toJson(src, typeOfSrc, writer)
    fun toJson(src: Any?, typeOfSrc: Type, writer: JsonWriter) = gson.toJson(src, typeOfSrc, writer)
    fun toJson(jsonElement: JsonElement): String = gson.toJson(jsonElement)
    fun toJson(jsonElement: JsonElement, writer: Appendable) = gson.toJson(jsonElement, writer)
    fun toJson(jsonElement: JsonElement, writer: JsonWriter) = gson.toJson(jsonElement, writer)

    private fun <T> enhance(anObject: T): T {
        var o = anObject
        enhancers.forEach { enhancer ->
            @Suppress("UNCHECKED_CAST")
            o = enhancer.invoke(o) as T
        }
        return o
    }

    private fun <T> check(anObject: T): T {
        return if (checks.any { check -> check.invoke(anObject) }) {
            throw IllegalArgumentException("Object:\n${gson.toJson(anObject)}\nContains illegal null values.")
        } else {
            anObject
        }
    }
}
