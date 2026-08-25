package top.fatweb.apimanagement.param.system

import com.baomidou.mybatisplus.annotation.EnumValue
import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Get active information parameters
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Schema(description = "用户活跃信息请求参数")
data class ActiveInfoGetParam(
    /**
     * Scope
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @field:Schema(
        description = "范围",
        allowableValues = ["WEEK", "MONTH", "QUARTER", "YEAR", "TWO_YEARS", "THREE_YEARS", "FIVE_YEARS", "ALL"],
        defaultValue = "WEEK",
        example = "WEEK"
    )
    val scope: Scope = Scope.WEEK
) {
    enum class Scope(@field:EnumValue @field:JsonValue val code: String) {
        WEEK("WEEK"),

        MONTH("MONTH"),

        QUARTER("QUARTER"),

        YEAR("YEAR"),

        TWO_YEARS("TWO_YEARS"),

        THREE_YEARS("THREE_YEARS"),

        FIVE_YEARS("FIVE_YEARS"),

        ALL("ALL")
    }
}
