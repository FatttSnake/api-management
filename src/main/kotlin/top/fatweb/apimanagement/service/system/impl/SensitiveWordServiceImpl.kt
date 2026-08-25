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
import top.fatweb.apimanagement.util.onCommitted
import top.fatweb.apimanagement.util.saveOrThrowException
import top.fatweb.apimanagement.util.updateOrThrowException
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
    /**
     * Enabled words as patterns that are already compiled
     *
     * Held because the check runs on the request path - registration and profile updates -
     * where reading the whole word list and compiling it again on every call costs far more
     * than the match it exists for. Null until first read, then invalidated by every write,
     * so a word takes effect as soon as the write that added it has committed.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    @Volatile
    private var compiledPatterns: List<Regex>? = null

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun get(): List<SensitiveWordVo> = list().map(SensitiveWord::toVo)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun add(sensitiveWordAddParam: SensitiveWordAddParam) {
        checkSensitiveWord(sensitiveWordAddParam.word!!)
        saveOrThrowException { save(sensitiveWordAddParam.toEntity()) }
        onCommitted { compiledPatterns = null }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun update(sensitiveWordUpdateParam: SensitiveWordUpdateParam) {
        val ids = sensitiveWordUpdateParam.ids

        // The submission is the whole enabled set: everything is switched off, then what was
        // submitted back on. Switching off asserts nothing, because a table that matches no
        // rows is already in the state it asks for - and an empty submission stops here
        // instead of reaching the second statement, whose `in ()` is not valid SQL
        update(
            KtUpdateWrapper(SensitiveWord())
                .set(SensitiveWord::enable, false)
        )

        if (ids.isNotEmpty()) {
            updateOrThrowException {
                update(
                    KtUpdateWrapper(SensitiveWord())
                        .`in`(SensitiveWord::id, ids)
                        .set(SensitiveWord::enable, true)
                )
            }
        }

        onCommitted { compiledPatterns = null }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun delete(id: Long) {
        removeById(id)
        onCommitted { compiledPatterns = null }
    }

    /**
     * Check whether the string matches an enabled word
     *
     * Reads no database state of its own: the words are matched against the compiled
     * snapshot, which a write refreshes once it commits.
     *
     * @param str String to check
     * @throws MatchSensitiveWordException when the string matches an enabled word
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    override fun checkSensitiveWord(str: String) {
        patterns().forEach {
            if (runCatching { it.matches(str) }.getOrDefault(false)) {
                throw MatchSensitiveWordException()
            }
        }
    }

    /**
     * Get the compiled enabled words, loading them on first use
     *
     * @return List of Regex object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private fun patterns(): List<Regex> = compiledPatterns ?: loadPatterns().also { compiledPatterns = it }

    /**
     * Load and compile the enabled words
     *
     * A word that is not a usable pattern is dropped rather than failing the check, which is
     * how a malformed entry behaved when it was compiled per call.
     *
     * @return List of Regex object
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Regex
     */
    private fun loadPatterns(): List<Regex> =
        list(
            KtQueryWrapper(SensitiveWord())
                .eq(SensitiveWord::enable, 1)
        )
            .mapNotNull(SensitiveWord::word)
            .mapNotNull { runCatching { Regex(it, RegexOption.IGNORE_CASE) }.getOrNull() }
}
