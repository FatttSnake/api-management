package top.fatweb.apimanagement.service.system.impl

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.converter.system.toEntity
import top.fatweb.apimanagement.converter.system.toVo
import top.fatweb.apimanagement.entity.system.SensitiveWord
import top.fatweb.apimanagement.exception.MatchSensitiveWordException
import top.fatweb.apimanagement.mapper.system.SensitiveWordMapper
import top.fatweb.apimanagement.param.system.SensitiveWordAddParam
import top.fatweb.apimanagement.param.system.SensitiveWordUpdateParam
import top.fatweb.apimanagement.service.system.ISensitiveWordService
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.vo.system.SensitiveWordVo

/**
 * Sensitive word service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see SensitiveWordMapper
 * @see SensitiveWord
 * @see ISensitiveWordService
 */
@Service
class SensitiveWordServiceImpl : ServiceImpl<SensitiveWordMapper, SensitiveWord>(), ISensitiveWordService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun get(): List<SensitiveWordVo> = this.list().map(SensitiveWord::toVo)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun add(sensitiveWordAddParam: SensitiveWordAddParam) {
        checkSensitiveWord(sensitiveWordAddParam.word!!)
        saveOrThrowException { this.save(sensitiveWordAddParam.toEntity()) }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun update(sensitiveWordUpdateParam: SensitiveWordUpdateParam) {
        saveOrThrowException {
            this.update(
                KtUpdateWrapper(SensitiveWord())
                    .set(SensitiveWord::enable, false)
            )
        }
        saveOrThrowException {
            this.update(
                KtUpdateWrapper(SensitiveWord())
                    .`in`(SensitiveWord::id, sensitiveWordUpdateParam.ids)
                    .set(SensitiveWord::enable, true)
            )
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun delete(id: Long) {
        this.removeById(id)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun checkSensitiveWord(str: String) {
        this.list(
            KtQueryWrapper(SensitiveWord())
                .eq(SensitiveWord::enable, 1)
        ).map(SensitiveWord::word).forEach {
            it ?: return@forEach

            try {
                if (Regex(it, RegexOption.IGNORE_CASE).matches(str)) {
                    throw MatchSensitiveWordException()
                }
            } catch (e: MatchSensitiveWordException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}
