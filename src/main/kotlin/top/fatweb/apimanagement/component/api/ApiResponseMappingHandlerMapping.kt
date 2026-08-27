package top.fatweb.apimanagement.component.api

import org.springframework.web.servlet.mvc.condition.RequestCondition
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import top.fatweb.apimanagement.annotation.ApiController
import java.lang.reflect.Method

/**
 * Api response mapping handler mapping
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RequestMappingHandlerMapping
 */
class ApiResponseMappingHandlerMapping : RequestMappingHandlerMapping() {
    private fun createCondition(clazz: Class<*>): RequestCondition<ApiVersionCondition>? =
        clazz.getAnnotation(ApiController::class.java)?.let {
            ApiVersionCondition(
                plugin = it.plugin,
                apiVersion = it.version
            )
        }

    override fun getCustomMethodCondition(method: Method): RequestCondition<*>? = createCondition(method.javaClass)

    override fun getCustomTypeCondition(handlerType: Class<*>): RequestCondition<*>? = createCondition(handlerType)
}
