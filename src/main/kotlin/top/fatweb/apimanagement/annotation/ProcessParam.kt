package top.fatweb.apimanagement.annotation

/**
 * Process parameter annotation
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProcessParam

/**
 * Parameter processor annotation
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ProcessStrategy
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ParamProcessor(
    val strategy: Array<ProcessStrategy> = [ProcessStrategy.Trim]
)

/**
 * Parameter process strategy
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
enum class ProcessStrategy {
    Trim,
    TrimStart,
    TrimEnd,
    ToUpper,
    ToLower,
    RemoveSpaces,
    Capitalize
}
