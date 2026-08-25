package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.EnumValue
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.time.LocalDateTime

/**
 * Statistics log entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_l_statistics_log")
class StatisticsLog : Serializable {
    enum class KeyItem(@field:EnumValue @field:JsonValue val code: String) {
        ONLINE_USERS_COUNT("ONLINE_USER_COUNT")
    }

    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * Key
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("key")
    var key: KeyItem? = null

    /**
     * Value
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("value")
    var value: String? = null

    /**
     * Record time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @TableField("record_time")
    var recordTime: LocalDateTime? = null

    override fun toString(): String {
        return "StatisticsLog(id=$id, key=$key, value=$value, recordTime=$recordTime)"
    }
}
