package com.taskbase.json

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test


class ArsonTest {

    private val jsonSerDe = Arson(gson = Gson())

    @Test
    fun testParseJson() {
        val o = jsonSerDe.fromJson("{'c':1}", SomeTestClass::class.java)
        assertEquals(SomeTestClass(c = 1), o)
    }
}

data class SomeTestClass(
    val s: String? = "default",
    val e: List<String> = listOf("a", "b", "c"),
    val c: Int
)