package top.fatweb.apimanagement.util

/**
 * Get random int value
 *
 * @param start Lower limit of random int value
 * @param end Upper limit of random int value
 * @return Random int value
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getRandomInt(start: Int = Int.MIN_VALUE, end: Int = Int.MAX_VALUE): Int {
    return (start..end).random()
}

/**
 * Get random long value
 *
 * @param start Lower limit of random long value
 * @param end Upper limit of random long value
 * @return Random long value
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun getRandomLong(start: Long = Long.MIN_VALUE, end: Long = Long.MAX_VALUE): Long {
    return (start..end).random()
}
