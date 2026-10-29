package top.fatweb.apimanagement.util

import com.baomidou.mybatisplus.core.metadata.OrderItem
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import top.fatweb.apimanagement.exception.DatabaseInsertException
import top.fatweb.apimanagement.exception.DatabaseUpdateException
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.param.PageSortParam

/**
 * Set sort parameters into page object
 *
 * @param pageSortParam Page sort parameters
 * @param page Page object
 * @param defaultOrder Default order by
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun <T : Page<*>> setPageSort(pageSortParam: PageSortParam?, page: T, defaultOrder: OrderItem? = null) {
    // The field is a column name, not SQL: anything outside that shape falls back to the
    // caller's default order instead of reaching the query
    val field = pageSortParam?.sortField?.takeIf { SORT_FIELD.matches(it) }
    if (field != null) {
        page.addOrder(
            if (pageSortParam.sortOrder?.startsWith("desc", true) == true) {
                OrderItem.desc(upperToUnderLetter(field))
            } else {
                OrderItem.asc(upperToUnderLetter(field))
            },
        )
    } else {
        defaultOrder?.let { page.addOrder(defaultOrder) }
    }
}

private val SORT_FIELD = Regex("^[A-Za-z][A-Za-z0-9_]{0,63}$")

/**
 * Execute database exists query operation, throw exception when not found
 *
 * @param exception Exception to throw
 * @param func Database exists query operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun existsOrThrowException(exception: RuntimeException? = null, func: () -> Boolean) {
    if (!func()) {
        throw exception ?: NoRecordFoundException()
    }
}

/**
 * Execute database query operation, throw exception when not found
 *
 * @param exception Exception to throw
 * @param func Database select operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun <T> queryOrThrowException(exception: RuntimeException? = null, func: () -> T?): T =
    func() ?: throw exception ?: NoRecordFoundException()

/**
 * Execute database insert operation, throw exception when failure
 *
 * @param exception Exception to throw
 * @param func Database insert operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun saveOrThrowException(exception: RuntimeException? = null, func: () -> Boolean) {
    if (!func()) {
        throw exception ?: DatabaseInsertException()
    }
}

/**
 * Execute database update operation, throw exception when failure
 *
 * @param exception Exception to throw
 * @param func Database update operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun updateOrThrowException(exception: RuntimeException? = null, func: () -> Boolean) {
    if (!func()) {
        throw exception ?: DatabaseUpdateException()
    }
}
