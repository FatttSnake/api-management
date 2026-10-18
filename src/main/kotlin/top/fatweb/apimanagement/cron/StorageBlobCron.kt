package top.fatweb.apimanagement.cron

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import top.fatweb.apimanagement.service.system.IStorageBlobService

/**
 * Storage blob scheduled tasks
 *
 * Content-addressed storage shares one object between every writer and counts the references,
 * so the last writer to let go cannot delete anything by itself - it would take the object out
 * from under a reader that still holds it. Reclaiming is therefore a sweep rather than a side
 * effect of a delete.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class StorageBlobCron(
    private val storageBlobService: IStorageBlobService
) {
    companion object {
        /**
         * How long a blob must have been unreferenced before it is deleted (1 hour)
         *
         * Long enough that no writer can be in the middle of storing the same content: storing
         * writes the bytes first and raises the count second, and reclaiming inside that window
         * would leave a fresh row pointing at bytes that were deleted under it.
         */
        private const val UNREFERENCED_GRACE_MILLIS = 60L * 60 * 1000

        /**
         * How often unreferenced blobs are swept, in cron form (second, minute, hour, day,
         * month, day of week)
         *
         * Daily and off the hour: nothing about the sweep is urgent, and reclaiming space does
         * not have to be prompt to be correct.
         */
        private const val RECLAIM_CRON = "0 13 4 * * *"
    }

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Delete the blobs nothing references any more
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see IStorageBlobService.reclaimUnreferenced
     */
    @Scheduled(cron = RECLAIM_CRON)
    fun reclaimUnreferencedBlobs() {
        val reclaimed = storageBlobService.reclaimUnreferenced(UNREFERENCED_GRACE_MILLIS)
        if (reclaimed > 0) {
            logger.info("Reclaimed {} unreferenced storage blob(s)", reclaimed)
        }
    }
}
