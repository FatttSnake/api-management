package top.fatweb.apimanagement.api.v1

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Decodes JSON bytes into a reified type.
 *
 * @param json JSON bytes
 * @return decoded value
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
inline fun <reified T> JsonCodec.decode(json: ByteArray): T = decode(json, typeOf<T>().toJavaType())

/**
 * Decodes JSON text into a reified type.
 *
 * @param json JSON text
 * @return decoded value
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
inline fun <reified T> JsonCodec.decode(json: String): T = decode(json, typeOf<T>().toJavaType())

/**
 * Maps a Kotlin type to a Java reflection type.
 *
 * @return Java reflection type
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@PublishedApi
internal fun KType.toJavaType(): Type {
    val classifier =
        classifier as? KClass<*> ?: throw IllegalArgumentException("Unsupported JSON type: $this")
    if (arguments.isEmpty()) {
        return classifier.java
    }
    val argumentTypes =
        arguments.map { argument ->
            argument.type?.toJavaType() ?: throw IllegalArgumentException("Unsupported JSON type: $this")
        }
    if (classifier.java.isArray) {
        return GenericArrayTypeImpl(argumentTypes.single())
    }
    return ParameterizedTypeImpl(classifier.java, argumentTypes.toTypedArray())
}

private class GenericArrayTypeImpl(private val componentType: Type) : GenericArrayType {
    override fun getGenericComponentType(): Type = componentType

    override fun getTypeName(): String = componentType.typeName + "[]"

    override fun toString(): String = typeName

    override fun equals(other: Any?): Boolean = other is GenericArrayType && componentType == other.genericComponentType

    override fun hashCode(): Int = componentType.hashCode()
}

private class ParameterizedTypeImpl(private val rawType: Type, private val actualTypeArguments: Array<Type>) :
    ParameterizedType {
    override fun getRawType(): Type = rawType

    override fun getActualTypeArguments(): Array<Type> = actualTypeArguments.copyOf()

    override fun getOwnerType(): Type? = null

    override fun getTypeName(): String =
        rawType.typeName + actualTypeArguments.joinToString(prefix = "<", postfix = ">") { it.typeName }

    override fun toString(): String = typeName

    override fun equals(other: Any?): Boolean = other is ParameterizedType && sameType(other)

    override fun hashCode(): Int = rawType.hashCode() xor actualTypeArguments.contentHashCode()

    private fun sameType(other: ParameterizedType): Boolean =
        rawType == other.rawType && actualTypeArguments.contentEquals(other.actualTypeArguments)
}
