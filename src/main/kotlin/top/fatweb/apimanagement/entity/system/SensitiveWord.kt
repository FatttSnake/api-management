package top.fatweb.apimanagement.entity.system

import com.baomidou.mybatisplus.annotation.EnumValue
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable

/**
 * Sensitive word entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_s_sensitive_word", autoResultMap = true)
class SensitiveWord : Serializable {
    enum class Use(@field:EnumValue @field:JsonValue val code: String) {
        USERNAME("USERNAME"), TITLE("TITLE");
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
     * Word
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("word")
    var word: String? = null

    /**
     * Use for
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField(value = "use_for", typeHandler = JacksonTypeHandler::class)
    @JvmField
    var useFor: Set<String>? = null

    /**
     * Enable
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("enable")
    var enable: Int? = null

    override fun toString(): String {
        return "SensitiveWord(id=$id, word=$word, useFor=$useFor, enable=$enable)"
    }
}
