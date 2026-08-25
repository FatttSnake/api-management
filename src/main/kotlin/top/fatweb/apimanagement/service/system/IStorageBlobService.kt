package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.StorageBlob

/**
 * Storage blob service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IService
 * @see StorageBlob
 */
interface IStorageBlobService : IService<StorageBlob> {
    /**
     * Load file from file storage
     *
     * @param fileHash File SHA-256 key
     * @return File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     */
    fun loadFile(fileHash: String): ByteArray?

    /**
     * Get file reference count
     *
     * @param fileHash File SHA-256 key
     * @return Reference count
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun getReferenceCount(fileHash: String): Long

    /**
     * Save file to file storage and update reference count
     *
     * @param data File content
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ByteArray
     */
    fun saveFile(data: ByteArray): String


    /**
     * Save file to file storage and update reference count
     *
     * @param str File content
     * @return File SHA-256 key
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun saveFile(str: String): String

    /**
     * Decrement file reference count
     *
     * @param fileHash File SHA-256 key
     * @return File reference count
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun removeFile(fileHash: String): Long
}
