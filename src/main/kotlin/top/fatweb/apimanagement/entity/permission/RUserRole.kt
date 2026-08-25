package top.fatweb.apimanagement.entity.permission

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable

/**
 * User role intermediate entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@TableName("t_r_user_role")
class RUserRole : Serializable {
    /**
     * ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableId("id")
    var id: Long? = null

    /**
     * User ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("user_id")
    var userId: Long? = null

    /**
     * Role ID
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("role_id")
    var roleId: Long? = null

    /**
     * Deleted
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("deleted")
    @TableLogic
    var deleted: Long? = null

    /**
     * Version
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @TableField("version")
    @Version
    var version: Int? = null

    override fun toString(): String {
        return "RUserRole(id=$id, userId=$userId, roleId=$roleId, deleted=$deleted, version=$version)"
    }
}
