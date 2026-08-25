package top.fatweb.apimanagement.aspectj

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.annotation.ParamProcessor
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.annotation.ProcessStrategy
import java.lang.reflect.Field

/**
 * Parameter process interceptor
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Aspect
@Component
class ParameterProcessInterceptor {
    /**
     * Parameter process pointcut
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Pointcut(
        """
        execution(* *(.., @org.springframework.web.bind.annotation.RequestBody (*), ..)) ||
        execution(* *(.., @org.springframework.web.bind.annotation.RequestParam (*), ..)) ||
        execution(* *(.., @org.springframework.web.bind.annotation.PathVariable (*), ..))
        """
    )
    fun processPointcut() {

    }

    /**
     * Do before parameter process pointcut
     *
     * @param joinPoint Join point
     * @return Arguments
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see JoinPoint
     */
    @Before("processPointcut()")
    fun doBefore(joinPoint: JoinPoint): Any {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val parameters = method.parameters
        val args = joinPoint.args

        val processedArgs = parameters.mapIndexed { index, parameter ->
            val arg = args[index]
            val needProcess = parameter.isAnnotationPresent(ProcessParam::class.java)

            if (arg != null && needProcess) {
                val parameterAnnotation = parameter.getAnnotation(ParamProcessor::class.java)
                val classAnnotation = arg.javaClass.getAnnotation(ParamProcessor::class.java)
                val effectiveAnnotation = parameterAnnotation ?: classAnnotation
                processArgument(arg = arg, annotation = effectiveAnnotation)
            } else {
                arg
            }
        }.toTypedArray()

        return processedArgs
    }

    private fun processArgument(arg: Any?, annotation: ParamProcessor?): Any? =
        when (arg) {
            null -> null
            is String -> if (annotation == null) {
                arg
            } else {
                processString(value = arg, strategies = annotation.strategy.toList())
            }

            is Collection<*> -> arg.map { processArgument(arg = it, annotation = annotation) }
            is Map<*, *> -> arg.mapValues { processArgument(arg = it.value, annotation = annotation) }
            is Array<*> -> arg.map { processArgument(arg = it, annotation = annotation) }
            else -> {
                if (isCustomClass(arg)) {
                    processObject(obj = arg, annotation = annotation)
                } else {
                    arg
                }
            }
        }

    private fun processString(value: String, strategies: List<ProcessStrategy>): String =
        strategies.fold(value) { acc, processStrategy ->
            when (processStrategy) {
                ProcessStrategy.Trim -> acc.trim()
                ProcessStrategy.TrimStart -> acc.trimStart()
                ProcessStrategy.TrimEnd -> acc.trimEnd()
                ProcessStrategy.ToUpper -> acc.uppercase()
                ProcessStrategy.ToLower -> acc.lowercase()
                ProcessStrategy.RemoveSpaces -> acc.replace(regex = "\\s+".toRegex(), replacement = "")
                ProcessStrategy.Capitalize -> acc.replaceFirstChar { it.uppercase() }
            }
        }

    private fun isCustomClass(obj: Any): Boolean =
        obj.javaClass.name.startsWith("top.fatweb.apimanagement.param.")

    private fun processObject(obj: Any, annotation: ParamProcessor?): Any {
        val fields = getAllFields(obj.javaClass)

        fields.forEach { field ->
            field.isAccessible = true
            val fieldValue = field.get(obj) ?: return@forEach
            val fieldAnnotation = field.getAnnotation(ParamProcessor::class.java)
            val effectiveAnnotation = fieldAnnotation ?: annotation

            val processedValue = processArgument(fieldValue, effectiveAnnotation)
            field.set(obj, processedValue)
        }

        return obj
    }

    private fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz

        while (currentClass != null && currentClass != Any::class.java) {
            fields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }

        return fields
    }
}
