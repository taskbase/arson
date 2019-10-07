package com.taskbase.arson.reflection

import java.util.*
import java.util.logging.Logger
import kotlin.collections.Map.Entry
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility.PUBLIC
import kotlin.reflect.full.declaredMemberProperties

object KotlinReflectionUtils {

    private val log: Logger = Logger.getLogger(KotlinReflectionUtils::class.qualifiedName)

    /**
     * Checks whether an object contains publicly accessible null values that are forbidden according to the Kotlin
     * type system. It can happen that objects are constructed through Java interoperability such that they circumvent
     * the Kotlin null pointer safety. This function checks whether this is the case.
     */
    fun <T> containsIllegalNullValues(anObject: T): Boolean {
        if (anObject != null) {
            when (anObject) {
                is Map<*, *> -> {
                    anObject.entries.forEach { entry: Entry<*, *> ->
                        if (entry.key == null
                            || entry.value == null
                            || containsIllegalNullValues(entry.key)
                            || containsIllegalNullValues(entry.value)
                        ) {
                            return true
                        }
                    }
                }
                is Iterable<*> -> {
                    anObject.forEach { element: Any? ->
                        if (element == null || containsIllegalNullValues(element)) {
                            return true
                        }
                    }
                }
                is Array<*> -> anObject.forEach { element: Any? ->
                    if (element == null || containsIllegalNullValues(element)) {
                        return true
                    }
                }
                else -> (anObject as Any?)!!::class.declaredMemberProperties
                    .filter { it.visibility == PUBLIC }
                    .forEach { member: KProperty1<*, *> ->
                        val memberValue = member.getter.call(anObject)
                        if (!member.returnType.isMarkedNullable && memberValue == null
                            || containsIllegalNullValues(memberValue)
                        ) {
                            log.warning("Member ${member.name} is illegally null!")
                            return true
                        }
                    }
            }
        }
        return false
    }

    /**
     * Create a new object that contains the same fields as the given object plus the default values.
     */
    fun <T> addDefaultValues(anObject: T): T {
        if (anObject != null) {
            @Suppress("UNCHECKED_CAST")
            return when (anObject) {
                is Map<*, *> -> {
                    anObject
                        .map { element: Entry<*, *> ->
                            Pair(addDefaultValues(element.key), addDefaultValues(element.value))
                        }.toMap()
                }
                is Iterable<*> -> {
                    anObject.map { element -> addDefaultValues(element) }
                }
                is Array<*> -> {
                    // Arrays are a special case. We have to be careful to not lose the type information.
                    val clonedArray = anObject.clone()
                    (0 until anObject.size).forEach { i ->
                        Arrays.fill(clonedArray, i, i + 1, addDefaultValues(anObject[i]))
                    }
                    clonedArray
                }
                else -> {
                    (anObject as? Any)?.let { anyObject: Any ->
                        if (isOfPrimitiveType(anyObject)) {
                            anyObject
                        } else {
                            // Select the constructor
                            val constructor: KFunction<Any>? = anyObject::class.constructors
                                .filter { c -> c.parameters.size == anyObject::class.declaredMemberProperties.size }
                                .firstOrNull { constructor: KFunction<Any> -> constructor.visibility == PUBLIC }
                            if (constructor == null) {
                                anyObject
                            } else {

                                // Compute the parameters for the constructor call
                                val kParameters: Map<String, KParameter> =
                                    constructor.parameters.map { Pair(it.name!!, it) }.toMap()

                                val params: Map<KParameter, Any?> = anyObject::class.declaredMemberProperties
                                    .filter { it.visibility == PUBLIC }
                                    .mapNotNull { member: KProperty1<out Any, *> ->
                                        val kParam = kParameters[member.name]
                                        if (kParam != null) {
                                            val value = addDefaultValues(member.getter.call(anyObject))
                                            if (kParam.isOptional && value == null) {
                                                // Leave out optional parameter as it will be constructed as default.
                                                null
                                            } else {
                                                Pair(kParam, value)
                                            }
                                        } else {
                                            null
                                        }
                                    }.toMap()

                                // Create a new object that includes the old values plus the default values.
                                constructor.callBy(params)
                            }
                        }
                    } ?: throw IllegalStateException()
                }
            } as T
        } else {
            return anObject
        }
    }

    private fun isOfPrimitiveType(o: Any): Boolean = primitiveTypes.any { it.isInstance(o) }

    private val primitiveTypes: Set<KClass<*>> = setOf(Number::class, String::class, Boolean::class, Char::class)
}
