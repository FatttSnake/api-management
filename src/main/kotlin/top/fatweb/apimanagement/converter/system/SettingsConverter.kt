package top.fatweb.apimanagement.converter.system

import top.fatweb.apimanagement.entity.system.SensitiveWord
import top.fatweb.apimanagement.param.system.SensitiveWordAddParam
import top.fatweb.apimanagement.vo.system.SensitiveWordVo

/**
 * Convert to SensitiveWordVo object
 *
 * @return SensitiveWordVo object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see SensitiveWord
 * @see SensitiveWordVo
 */
fun SensitiveWord.toVo() = SensitiveWordVo(
    id = this.id,
    word = this.word,
    useFor = this.useFor?.map(SensitiveWord.Use::valueOf)?.toSet(),
    enable = this.enable?.let { it == 1 }
)

/**
 * Convert to SensitiveWord object
 *
 * @return SensitiveWord object
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see SensitiveWordAddParam
 * @see SensitiveWord
 */
fun SensitiveWordAddParam.toEntity() = SensitiveWord().apply {
    word = this@toEntity.word
    useFor = this@toEntity.useFor.map(SensitiveWord.Use::code).toSet()
    enable = if (this@toEntity.enable) 1 else 0
}
