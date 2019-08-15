package com.taskbase.arson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class ArsonTest {

    @Test
    fun testEnumDeserialization() {
        val json = "{'name':'Jim', 'hairColor':'3'}"

        // Gson deserializes the value to null
        val p1 = Gson().fromJson(json, Person::class.java)
        assertNull(p1.hairColor)

        // The wrapper replaces null with the default value
        val p2 = Arson(gson = Gson()).fromJson(json, Person::class.java)
        assertEquals(Color.NONE, p2.hairColor)
    }
}

data class Person(
    val name: String = "",
    val age: Int = 0,
    val hairColor: Color = Color.NONE
)

enum class Color {
    @SerializedName("1")
    BROWN,
    @SerializedName("2")
    BLONDE,
    NONE
}
