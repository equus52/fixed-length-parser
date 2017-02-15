package co.stepo.fixedlength

import java.math.BigDecimal
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaSetter
import kotlin.reflect.memberProperties


@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Field(val position: Int, val length: Int, val deserializer: String = "")


class FixedLengthParser<T : Any>(
        val kClass: KClass<T>,
        val config: Config = Config(),
        val deserializerMap: Map<String, (String) -> Any> = emptyMap()
) {
    val constructor: KFunction<T>
    val properties: List<KMutableProperty1<T, *>>

    init {
        val availableConstructors = kClass.constructors.filter { it.parameters.all { it.annotations.hasField() || it.isOptional } }
        constructor = availableConstructors.maxBy { it.parameters.size } ?: throw RuntimeException()

        val mutableProperties = kClass.memberProperties.flatMap { if (it is KMutableProperty1<T, *>) listOf(it) else emptyList() }
        properties = mutableProperties.filter { it.annotations.hasField() }
    }

    fun parse(text: String): T {
        val instance = createInstance(text)

        properties.forEach {
            val type = it.javaSetter?.parameterTypes?.single() ?: throw RuntimeException()
            val field = it.annotations.field
            val value = deserialize(type, text, field)
            it.setter.call(instance, value)
        }

        return instance
    }

    private fun createInstance(text: String): T {
        val params = constructor.javaConstructor?.parameters?.map { deserialize(it.type, text, it.getAnnotation(Field::class.java)) } ?: throw RuntimeException()
        return constructor.call(*params.toTypedArray())
    }

    private fun deserialize(type: Class<*>, text: String, field: Field): Any {
        val strValue = text.substring(field).let { if (config.isEnabledTrim) it.trim() else it }
        if (field.deserializer.isNotBlank()) {
            val deserializer = deserializerMap[field.deserializer] ?: throw RuntimeException()
            return deserializer(strValue)
        }
        return strValue.deserializeTo(type)
    }

    private fun List<Annotation>.hasField(): Boolean {
        return this.find { it is Field } != null
    }

    private val List<Annotation>.field: Field
        get() = this.find { it is Field } as Field? ?: throw RuntimeException()

    private fun String.substring(field: Field): String {
        return if (config.isByteIndex) {
            this.substringByByte(config.charSet, field.position - 1, field.position + field.length - 1)
        } else {
            this.substring(field.position - 1, field.position + field.length - 1)
        }
    }


    private fun String.deserializeTo(type: Class<*>): Any {
        return when (type) {
            String::class.java -> this
            Byte::class.java -> toByte()
            Short::class.java -> toShort()
            Int::class.java -> toInt()
            Long::class.java -> toLong()
            Float::class.java -> toFloat()
            Double::class.java -> toDouble()
            BigDecimal::class.java -> BigDecimal(this)
            Boolean::class.java -> toBoolean()
            ByteArray::class.java -> toByteArray()
            else -> throw RuntimeException()
        }
    }

    data class Config(
            val isEnabledTrim: Boolean = false,
            val isByteIndex: Boolean = false,
            val charSet: Charset = Charsets.UTF_8
    )
}

fun String.substringByByte(charset: Charset, startByteIndex: Int, endByteIndex: Int): String {
    val bytes = this.toByteArray(charset).filterIndexed { i, byte -> i >= startByteIndex && i < endByteIndex }.toByteArray()
    return String(bytes, charset)
}