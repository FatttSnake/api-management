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
 * Event log entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_l_event_log")
class EventLog : Serializable {
    enum class Event(@field:EnumValue @field:JsonValue val code: String) {
        LOGIN("LOGIN"), LOGOUT("LOGOUT"), REGISTER("REGISTER"), VERIFY("VERIFY"), API("API"),
        KEY_CREATE("KEY_CREATE"), KEY_UPDATE("KEY_UPDATE"), KEY_DELETE("KEY_DELETE"), KEY_STATUS("KEY_STATUS"),
        KEY_REGENERATE("KEY_REGENERATE"), KEY_TOPUP("KEY_TOPUP")
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
     * Event
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("event")
    var event: Event? = null

    /**
     * Operate user ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("operate_user_id")
    var operateUserId: Long? = null

    /**
     * Operate time
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @TableField("operate_time")
    var operateTime: LocalDateTime? = null

    /**
     * Event detail
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("detail")
    var detail: String? = null

    override fun toString(): String {
        return "EventLog(id=$id, event=$event, operateUserId=$operateUserId, operateTime=$operateTime, detail=$detail)"
    }
}
