package top.fatweb.apimanagement.aspectj

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.annotation.EventLogRecord
import top.fatweb.apimanagement.entity.system.EventLog
import top.fatweb.apimanagement.param.system.apiAccount.ApiTopUpParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyDeleteParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyUpdateParam
import top.fatweb.apimanagement.param.system.apiKey.ApiKeyUpdateStatusParam
import top.fatweb.apimanagement.service.system.IEventLogService
import top.fatweb.apimanagement.util.getLoginUserId
import top.fatweb.apimanagement.vo.api.ApiKeyWithSecretVo
import top.fatweb.apimanagement.vo.api.ApiTransactionVo
import top.fatweb.apimanagement.vo.permission.LoginVo
import top.fatweb.apimanagement.vo.permission.RegisterVo

/**
 * Event log record interceptor
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IEventLogService
 */
@Aspect
@Component
class EventLogInterceptor(
    private val eventLogService: IEventLogService
) {
    /**
     * Event log record pointcut
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Pointcut("@annotation(top.fatweb.apimanagement.annotation.EventLogRecord)")
    fun eventLogPointcut() {
    }

    /**
     * Do after event log record pointcut
     *
     * @param joinPoint Join point
     * @param retValue Return value
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see JoinPoint
     */
    @AfterReturning(value = "eventLogPointcut()", returning = "retValue")
    fun doAfter(joinPoint: JoinPoint, retValue: Any?) {
        val annotation = (joinPoint.signature as MethodSignature).method.getAnnotation(EventLogRecord::class.java)

        val userId = getLoginUserId() ?: when (retValue) {
            is LoginVo -> retValue.userId!!
            is RegisterVo -> retValue.userId!!
            else -> -1
        }

        val keyLog = resolveKeyLog(joinPoint.args, annotation!!.event, retValue)
        eventLogService.saveEvent(
            annotation = annotation,
            userId = userId,
            apiKeyId = keyLog.apiKeyId,
            targetUserId = keyLog.targetUserId,
            detail = keyLog.detail
        )
    }

    /**
     * Resolve the affected object references (still stored as IDs) and a readable detail
     *
     * @param args Method arguments
     * @param event Event type
     * @param retValue Return value
     * @return Resolved references and detail
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private fun resolveKeyLog(
        args: Array<Any>,
        event: EventLog.Event,
        retValue: Any?
    ): KeyLog {
        return when (event) {
            EventLog.Event.KEY_CREATE, EventLog.Event.KEY_REGENERATE -> {
                val keyVo = (retValue as? ApiKeyWithSecretVo)?.apiKey
                val apiKeyId = keyVo?.id
                val action = if (event == EventLog.Event.KEY_CREATE) "创建" else "重新生成"
                KeyLog(
                    apiKeyId = apiKeyId,
                    detail = if (keyVo == null) "$action Key" else "$action Key「${keyVo.name.orEmpty()}」(id=${apiKeyId})"
                )
            }

            EventLog.Event.KEY_UPDATE -> {
                val apiKeyId = args.firstNotNullOfOrNull { (it as? ApiKeyUpdateParam)?.id }
                KeyLog(apiKeyId = apiKeyId, detail = "更新 Key 配置(id=$apiKeyId)")
            }

            EventLog.Event.KEY_STATUS -> {
                val apiKeyId = args.firstNotNullOfOrNull { (it as? ApiKeyUpdateStatusParam)?.id }
                KeyLog(apiKeyId = apiKeyId, detail = "修改 Key 启停状态(id=$apiKeyId)")
            }

            EventLog.Event.KEY_DELETE -> {
                val ids = args.firstNotNullOfOrNull { (it as? ApiKeyDeleteParam)?.ids }
                KeyLog(
                    apiKeyId = ids?.firstOrNull(),
                    detail = "删除 Key ${ids?.size ?: 0} 个${ids?.takeIf { list -> list.size > 1 }?.let { "（含 id=${ids.first()} 等）" } ?: ""}"
                )
            }

            EventLog.Event.KEY_TOPUP -> {
                val transactionVo = retValue as? ApiTransactionVo
                val targetUserId = transactionVo?.userId ?: args.firstNotNullOfOrNull { (it as? ApiTopUpParam)?.userId }
                val orderNo = transactionVo?.orderNo
                KeyLog(
                    targetUserId = targetUserId,
                    detail = "账户充值${orderNo?.let { "（订单 $it）" } ?: ""}"
                )
            }

            else -> KeyLog()
        }
    }

    /**
     * Resolved event references
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    private data class KeyLog(
        val apiKeyId: Long? = null,
        val targetUserId: Long? = null,
        val detail: String? = null
    )
}
