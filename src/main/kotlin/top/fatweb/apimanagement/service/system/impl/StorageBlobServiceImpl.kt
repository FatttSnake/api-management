package top.fatweb.apimanagement.service.system.impl

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.entity.system.StorageBlob
import top.fatweb.apimanagement.mapper.system.StorageBlobMapper
import top.fatweb.apimanagement.service.system.IStorageBlobService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * Storage blob service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Service
class StorageBlobServiceImpl(
    private val fileStorageProvider: FileStorageProvider
) : ServiceImpl<StorageBlobMapper, StorageBlob>(), IStorageBlobService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    override fun loadFile(fileHash: String): ByteArray? =
        fileStorageProvider.load(fileHash)

    override fun existsFile(fileHash: String): Boolean =
        fileStorageProvider.exists(fileHash)

    override fun getReferenceCount(fileHash: String): Long =
        baseMapper.selectById(fileHash)?.referenceCount ?: 0

    @Transactional
    override fun saveFile(data: ByteArray): String {
        val fileHash = fileStorageProvider.save(data)
        baseMapper.increaseReferenceCount(fileHash)

        return fileHash
    }

    @Transactional
    override fun saveFile(str: String): String =
        saveFile(str.toByteArray())

    @Transactional
    override fun removeFile(fileHash: String): Long {
        baseMapper.update(
            KtUpdateWrapper(StorageBlob())
                .eq(StorageBlob::fileHash, fileHash)
                .gt(StorageBlob::referenceCount, 0)
                .setSql("reference_count = reference_count - 1")
        )

        return getReferenceCount(fileHash)
    }

    override fun reclaimUnreferenced(graceMillis: Long): Int {
        // Compared against the stored timestamps, which are written with utc_timestamp()
        val cutoff = LocalDateTime.now(ZoneOffset.UTC)
            .minusNanos(TimeUnit.MILLISECONDS.toNanos(graceMillis))

        val candidates = list(
            KtQueryWrapper(StorageBlob())
                .le(StorageBlob::referenceCount, 0)
                .lt(StorageBlob::updateTime, cutoff)
        )

        var reclaimed = 0
        candidates.forEach { blob ->
            val fileHash = blob.fileHash ?: return@forEach

            // Given up conditionally, so a blob that was referenced again between the read
            // above and here keeps both its row and its bytes. The row goes first for that
            // reason: bytes deleted for a blob a live row still points at would be unrecoverable,
            // while a row deleted before its bytes only wastes them
            val forgotten = baseMapper.delete(
                KtQueryWrapper(StorageBlob())
                    .eq(StorageBlob::fileHash, fileHash)
                    .le(StorageBlob::referenceCount, 0)
            ) > 0
            if (!forgotten) {
                return@forEach
            }

            runCatching { fileStorageProvider.delete(fileHash) }.onFailure {
                logger.warn("Failed to delete the bytes of unreferenced blob '{}': {}", fileHash, it.message)
            }
            reclaimed++
        }

        return reclaimed
    }
}
