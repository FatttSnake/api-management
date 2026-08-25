package top.fatweb.apimanagement.service.system.impl

import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import top.fatweb.apimanagement.component.storage.FileStorageProvider
import top.fatweb.apimanagement.entity.system.StorageBlob
import top.fatweb.apimanagement.mapper.system.StorageBlobMapper
import top.fatweb.apimanagement.service.system.IStorageBlobService

/**
 * Storage blob service implement
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see ServiceImpl
 * @see StorageBlobMapper
 * @see StorageBlob
 * @see IStorageBlobService
 */
@Service
class StorageBlobServiceImpl(
    private val fileStorageProvider: FileStorageProvider
) : ServiceImpl<StorageBlobMapper, StorageBlob>(), IStorageBlobService {
    override fun loadFile(fileHash: String): ByteArray? =
        fileStorageProvider.load(fileHash)

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
        this.saveFile(str.toByteArray())

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
}
