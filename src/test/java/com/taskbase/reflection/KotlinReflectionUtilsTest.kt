package com.taskbase.reflection

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinUtilsTest {

    @Test
    fun testValidObject() {
        val deserialized = KotlinReflectionUtils.addDefaultValues(
            Gson().fromJson(
                "{'a':'foo','b':42,'c':['foo','bar'],'map':{'a':'b'}}",
                NonNullableType::class.java
            )
        )
        assertEquals("test", deserialized.defaultValue)
        assertFalse(KotlinReflectionUtils.containsIllegalNullValues(deserialized))
    }

    @Test
    fun testNullValueInField() {
        val faultyObject = Gson().fromJson("{'b':42,'c':['foo','bar'],'map':{'a':'b'}}", NonNullableType::class.java)
        assertNull(faultyObject.a)
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(faultyObject))
    }

    @Test
    fun testNullValueInList() {
        val faultyObject =
            Gson().fromJson("{'a':'foo','b':42,'c':['foo',,'bar'],'map':{'a':'b'}}", NonNullableType::class.java)
        assertNull(faultyObject.c[1])
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(faultyObject))
    }

    @Test
    fun testCheckMap() {
        val map = mapOf(Pair("Hey", "Joe"))
        assertFalse(KotlinReflectionUtils.containsIllegalNullValues(map))
    }

    @Test
    fun testIllegalNullValueInMap() {
        val faultyObject =
            Gson().fromJson("{'a':'foo','b':42,'c':['foo','bar'],'map':{'a':null}}", NonNullableType::class.java)
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(faultyObject))
        assertNull(faultyObject.map["a"])
    }

    @Test
    fun testIllegalNullValueInArray() {
        val array = arrayOf(
            Gson().fromJson(
                "{'a':'foo','b':42,'c':['foo',,'bar'],'map':{'a':'b'}}",
                NonNullableType::class.java
            )
        )
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(array))
    }

    @Test
    fun testAddDefaultValue() {
        val originObject = NonNullableType(b = 7, optionalParam = "some-string")
        val enhancedObject = KotlinReflectionUtils.addDefaultValues(originObject)
        assertNotNull(enhancedObject.defaultValue)
        assertEquals(originObject.defaultValue, enhancedObject.defaultValue)
        assertEquals(originObject, enhancedObject)
    }

    @Test
    fun testAddDefaultValuesToList() {
        val list: List<NonNullableType> = listOf(
            Gson().fromJson(
                "{'a':'foo','b':42,'c':['foo',,'bar'],'map':{'a':'b'}}",
                NonNullableType::class.java
            )
        )
        val enhancedArray: List<NonNullableType> = KotlinReflectionUtils.addDefaultValues(list)
        enhancedArray.forEach {
            assertEquals("test", it.defaultValue)
        }
    }

    @Test
    fun testAddDefaultValuesToArray() {
        val array: Array<NonNullableType> = arrayOf(
            Gson().fromJson(
                "{'a':'foo','b':42,'c':['foo',,'bar'],'map':{'a':'b'}}",
                NonNullableType::class.java
            )
        )
        val enhancedArray: Array<NonNullableType> = KotlinReflectionUtils.addDefaultValues(array)
        enhancedArray.forEach {
            assertEquals("test", it.defaultValue)
        }
    }

    @Test
    fun testNestedDefaultValue() {
        val faultyObject =
            Gson().fromJson("{'a':'foo','b':42,'c':['foo','bar'],'map':{'a':'b'}}", NonNullableType::class.java)
        assertNull(faultyObject.x)
        val fixedObject = KotlinReflectionUtils.addDefaultValues(faultyObject)
        assertEquals("nested", fixedObject.x.x)
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(faultyObject))
        assertFalse(KotlinReflectionUtils.containsIllegalNullValues(fixedObject))
    }

    @Test
    fun testNestedDefaultValueWithEmptyNestedObject() {
        val faultyObject =
            Gson().fromJson("{'a':'foo','b':42,'c':['foo','bar'],'map':{'a':'b'},'x':{}}", NonNullableType::class.java)
        val fixedObject = KotlinReflectionUtils.addDefaultValues(faultyObject)
        assertEquals("nested", fixedObject.x.x)
        assertTrue(KotlinReflectionUtils.containsIllegalNullValues(faultyObject))
        assertFalse(KotlinReflectionUtils.containsIllegalNullValues(fixedObject))
    }
}

data class NonNullableType(
    val b: Int,
    val a: String = "a",
    val defaultValue: String = "test",
    val map: Map<String, String> = emptyMap(),
    val c: List<String> = emptyList(),
    val x: Nested = Nested(child = null),
    val optionalParam: String?,
    val optionalParamWithDefault: String? = "optional"
)

data class Nested(
    val x: String = "nested",
    val child: Nested?
)
