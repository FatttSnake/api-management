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

    /**
     * Delete every blob nothing references any more
     *
     * [removeFile] only gives up a reference, because the bytes have to outlive the last reader
     * that is still using them. This is what eventually takes the object itself: a blob whose
     * count reached zero and has stayed there for [graceMillis] has its row and its bytes
     * deleted.
     *
     * The wait is what keeps a re-save safe. Storing content writes the bytes and then raises
     * the count, so a blob that is deleted in between those two steps would leave a row
     * pointing at bytes that are gone; a grace period far longer than that window makes it
     * something that cannot happen rather than something that is unlikely.
     *
     * @param graceMillis How long a blob must have been unreferenced before it is deleted
     * @return Number of blobs reclaimed
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun reclaimUnreferenced(graceMillis: Long): Int
}
