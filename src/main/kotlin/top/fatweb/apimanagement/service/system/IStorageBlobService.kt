package top.fatweb.apimanagement.service.system

import com.baomidou.mybatisplus.spring.service.IService
import top.fatweb.apimanagement.entity.system.StorageBlob

/**
 * Storage blob service interface
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
interface IStorageBlobService : IService<StorageBlob> {
    /**
     * Load file from file storage
     *
     * @param fileHash File SHA-256 key
     * @return File content
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun loadFile(fileHash: String): ByteArray?

    /**
     * Check if the file is stored in file storage
     *
     * @param fileHash File SHA-256 key
     * @return true=exist; false=not exist
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun existsFile(fileHash: String): Boolean

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

    /**
     * Delete every blob nothing references any more
     *
     * [removeFile] only gives up a reference; a blob whose count reached zero and has stayed
     * there for [graceMillis] has its row and its bytes deleted.
     *
     * @param graceMillis How long a blob must have been unreferenced before it is deleted
     * @return Number of blobs reclaimed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun reclaimUnreferenced(graceMillis: Long): Int
}
