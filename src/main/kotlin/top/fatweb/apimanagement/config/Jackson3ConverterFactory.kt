package top.fatweb.apimanagement.config

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import tools.jackson.databind.json.JsonMapper
import java.lang.reflect.Type

/**
 * A Retrofit converter factory backed by a Jackson 3 [JsonMapper].
 *
 * Retrofit's official `converter-jackson` only supports Jackson 2 (`com.fasterxml.jackson`),
 * so Spring Boot 4, which defaults to Jackson 3 (`tools.jackson`), needs a converter
 * of its own.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see JsonMapper
 * @see Converter
 */
class Jackson3ConverterFactory private constructor(
    private val mapper: JsonMapper
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val javaType = mapper.constructType(type)
        return Converter<ResponseBody, Any> { body ->
            body.charStream().use { mapper.readerFor(javaType).readValue(it) }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        return Converter<Any, RequestBody> { value ->
            mapper.writeValueAsBytes(value).toRequestBody(MEDIA_TYPE)
        }
    }

    companion object {
        private val MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()

        fun create(mapper: JsonMapper): Jackson3ConverterFactory = Jackson3ConverterFactory(mapper)
    }
}
