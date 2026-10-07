package top.fatweb.apimanagement.component.plugin

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Plugin in-flight tracker tests
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginInFlightTracker
 */
class PluginInFlightTrackerTests {
    private companion object {
        const val PLUGIN_ID = "echo"
        const val NO_WAIT_MILLIS = 50L
    }

    private val tracker = PluginInFlightTracker()

    @Test
    fun `a plugin with nothing in flight drains at once`() {
        assertTrue(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `an entered request is waited for`() {
        val handle = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(handle)

        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))

        tracker.exit(handle)
        assertTrue(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `draining refuses a request that has not entered yet`() {
        tracker.beginDrain(PLUGIN_ID)

        assertNull(tracker.tryEnter(PLUGIN_ID))
    }

    @Test
    fun `a request already inside is counted even after the drain begins`() {
        val handle = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(handle)

        tracker.beginDrain(PLUGIN_ID)

        // Refused to newcomers, and still waiting on the one that got in before
        assertNull(tracker.tryEnter(PLUGIN_ID))
        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))

        tracker.exit(handle)
        assertTrue(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `draining waits for a request that finishes later`() {
        val handle = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(handle)

        val exit = CountDownLatch(1)
        val releaser = thread {
            exit.await()
            tracker.exit(handle)
        }
        val drained = CountDownLatch(1)
        val waiter = thread {
            drained.countDown()
            assertTrue(tracker.awaitDrain(PLUGIN_ID, TimeUnit.SECONDS.toMillis(5)))
        }

        drained.await()
        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))

        exit.countDown()
        waiter.join(TimeUnit.SECONDS.toMillis(30))
        releaser.join(TimeUnit.SECONDS.toMillis(30))

        assertTrue(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `a request that outlives the timeout does not hold the drain open`() {
        val handle = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(handle)

        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `a released plugin starts from a clean count`() {
        val beforeUnmount = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(beforeUnmount)

        tracker.beginDrain(PLUGIN_ID)
        tracker.release(PLUGIN_ID)

        // The next mount is enterable again, which is what release is for
        val afterRemount = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(afterRemount)
    }

    @Test
    fun `a straggler does not count against the next mount`() {
        // The drain gave up on its timeout and tore the plugin down anyway; the request it
        // stopped waiting for finishes afterwards, and must not leave the next mount looking
        // like it still has one in flight
        val straggler = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(straggler)

        tracker.beginDrain(PLUGIN_ID)
        tracker.release(PLUGIN_ID)

        val nextMount = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(nextMount)

        tracker.exit(straggler)

        // Still one, the request the new mount admitted - not zero, and not minus one
        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))

        tracker.exit(nextMount)
        assertTrue(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }

    @Test
    fun `an exit without an enter is ignored`() {
        val handle = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(handle)

        tracker.exit(handle)
        tracker.exit(handle)

        // Counted down to zero, not past it: a double exit must not make a later request look
        // like it has company
        val next = tracker.tryEnter(PLUGIN_ID)
        assertNotNull(next)
        assertFalse(tracker.awaitDrain(PLUGIN_ID, NO_WAIT_MILLIS))
    }
}
