package top.fatweb.apimanagement.handler

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import org.apache.ibatis.reflection.MetaObject
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Date meta object handler
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see MetaObjectHandler
 */
@Component
class DataMetaObjectHandler : MetaObjectHandler {
    override fun insertFill(metaObject: MetaObject?) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::class.java, LocalDateTime.now(ZoneOffset.UTC))
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::class.java, LocalDateTime.now(ZoneOffset.UTC))
    }

    override fun updateFill(metaObject: MetaObject?) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::class.java, LocalDateTime.now(ZoneOffset.UTC))
    }
}
