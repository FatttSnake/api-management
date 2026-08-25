package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.SensitiveWord
import top.fatweb.apimanagement.param.system.SensitiveWordAddParam
import top.fatweb.apimanagement.param.system.SensitiveWordUpdateParam
import top.fatweb.apimanagement.vo.system.SensitiveWordVo

/**
 * Sensitive word service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see SensitiveWord
 */
interface ISensitiveWordService : IService<SensitiveWord> {
    /**
     * Get sensitive word settings
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWordVo
     */
    fun get(): List<SensitiveWordVo>

    /**
     * Add sensitive word settings item
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWordAddParam
     */
    fun add(sensitiveWordAddParam: SensitiveWordAddParam)

    /**
     * Update sensitive word settings
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see SensitiveWordUpdateParam
     */
    fun update(sensitiveWordUpdateParam: SensitiveWordUpdateParam)

    /**
     * Delete sensitive word settings item
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun delete(id: Long)

    /**
     * Check sensitive word
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun checkSensitiveWord(str: String)
}
