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
 * @see Page
 * @see PageSortParam
 * @see OrderItem
 */
fun <T : Page<*>> setPageSort(pageSortParam: PageSortParam?, page: T, defaultOrder: OrderItem? = null) {
    if (pageSortParam?.sortField != null || pageSortParam?.sortOrder != null) {
        page.addOrder(
            if (pageSortParam.sortOrder?.startsWith(
                    "desc", true
                ) == true
            ) OrderItem.desc(upperToUnderLetter(pageSortParam.sortField)) else OrderItem.asc(
                upperToUnderLetter(
                    pageSortParam.sortField
                )
            )
        )
    } else {
        defaultOrder?.let { page.addOrder(defaultOrder) }
    }
}

/**
 * Execute database exists query operation, throw exception when not found
 *
 * @param exception Exception to throw
 * @param func Database exists query operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
fun existsOrThrowException(exception: RuntimeException = NoRecordFoundException(), func: () -> Boolean) {
    if (!func()) {
        throw exception
    }
}

/**
 * Execute database query operation, throw exception when not found
 *
 * @param exception Exception to throw
 * @param func Database select operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
 */
fun <T> queryOrThrowException(exception: RuntimeException? = null, func: () -> T?): T {
    return func() ?: throw exception ?: NoRecordFoundException()
}

/**
 * Execute database insert operation, throw exception when failure
 *
 * @param exception Exception to throw
 * @param func Database insert operation function
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see RuntimeException
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
 * @see RuntimeException
 */
fun updateOrThrowException(exception: RuntimeException? = null, func: () -> Boolean) {
    if (!func()) {
        throw exception ?: DatabaseUpdateException()
    }
}
