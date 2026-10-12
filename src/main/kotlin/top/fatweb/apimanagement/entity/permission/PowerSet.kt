package top.fatweb.apimanagement.entity.permission

import java.io.Serializable

/**
 * Set of power entity
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
class PowerSet : Serializable {
    /**
     * Module list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var moduleList: List<Module>? = null

    /**
     * Menu list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var menuList: List<Menu>? = null

    /**
     * Scope list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var scopeLists: List<Scope>? = null

    /**
     * Operation list
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    var operationList: List<Operation>? = null

    override fun toString(): String {
        return "PowerSet(moduleList=$moduleList, menuList=$menuList, scopeLists=$scopeLists, operationList=$operationList)"
    }
}
