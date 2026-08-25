package top.fatweb.apimanagement.mapper.system

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.baomidou.mybatisplus.core.metadata.IPage
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import top.fatweb.apimanagement.entity.system.SysLog
import java.time.LocalDateTime

/**
 * System log mapper
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see BaseMapper
 * @see SysLog
 */
@Mapper
interface SysLogMapper : BaseMapper<SysLog> {
    /**
     * Select system log in page
     *
     * @param page Pagination
     * @param logType List of log types
     * @param traceId Trace ID
     * @param requestMethod List of request methods
     * @param searchRequestUrl Request URL to search for
     * @param searchStartTime Start time to search for
     * @param searchEndTime end time to search for
     * @return System log in page
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see IPage
     * @see LocalDateTime
     * @see SysLog
     */
    fun selectPage(
        page: IPage<SysLog>,
        @Param("logType") logType: List<String>?,
        @Param("traceId") traceId: String?,
        @Param("requestMethod") requestMethod: List<String>?,
        @Param("searchRequestUrl") searchRequestUrl: String?,
        @Param("searchStartTime") searchStartTime: LocalDateTime?,
        @Param("searchEndTime") searchEndTime: LocalDateTime?
    ): IPage<SysLog>
}
