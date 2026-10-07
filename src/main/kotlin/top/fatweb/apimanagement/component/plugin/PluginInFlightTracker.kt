package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Plugin in-flight request tracker
 *
 * Counts the requests currently executing inside each plugin, so that tearing a plugin down can
 * wait for them instead of closing the child context, the datasource pools and the class loader
 * out from under code that is still running. An unmount has no other way to know: the dispatcher
 * stops handing out new requests the moment a mapping is unregistered, but a request already
 * past that point keeps running on its own thread.
 *
 * A request enters once, at the end of the interceptor's admission - after every check that can
 * refuse it - and leaves in `afterCompletion`. Entering any earlier would count requests that
 * never reach the plugin, and any interception that refuses after entering would leave a count
 * that nothing ever decrements.
 *
 * The handle a request gets back is what it leaves by, rather than its plugin ID. A drain that
 * gives up on its timeout tears the plugin down anyway, and the state is then discarded: a
 * straggler that finishes afterwards decrements the state it entered on, which nothing is
 * waiting on any more, instead of the fresh state belonging to the plugin's next mount.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see PluginInFlightTracker.Handle
 */
@Component
class PluginInFlightTracker {
    /**
     * The admission a request was granted
     *
     * Opaque on purpose: it identifies which state the request counted itself into, and the
     * only thing a caller does with it is hand it back to [exit].
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see PluginInFlightTracker
     */
    class Handle internal constructor(
        /**
         * The state this request counted itself into
         *
         * @author FatttSnake, fatttsnake@gmail.com
         * @since 1.0.0
         * @see State
         */
        internal val state: State
    )

    /**
     * Per-plugin admission state
     *
     * The count is the shared fact and the lock only guards it, so a request never holds
     * anything for the duration of its work - which also keeps the tracker usable from a
     * request that completes on a different thread than the one that admitted it.
     *
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    internal class State {
        val lock = ReentrantLock()
        val idle = lock.newCondition()
        var count = 0
        var draining = false
    }

    private val states = ConcurrentHashMap<String, State>()

    /**
     * Admit a request to a plugin
     *
     * @param pluginId Plugin ID
     * @return Handle to leave by, or null while the plugin is being torn down - the caller
     *         refuses the request rather than running it against a plugin on its way out
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see Handle
     */
    fun tryEnter(pluginId: String): Handle? {
        val state = states.computeIfAbsent(pluginId) { State() }

        state.lock.lock()
        try {
            // The state can be discarded between the lookup above and the lock, with a fresh one
            // installed for the plugin's next mount. A request that landed on the old one
            // belongs to neither mount, so it is refused rather than counted against either.
            if (states[pluginId] !== state || state.draining) {
                return null
            }

            state.count++

            return Handle(state)
        } finally {
            state.lock.unlock()
        }
    }

    /**
     * Stop counting a request
     *
     * @param handle Handle returned by [tryEnter]
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see tryEnter
     */
    fun exit(handle: Handle) {
        val state = handle.state

        state.lock.lock()
        try {
            // Guarded so a request that leaves twice cannot pass a drain that is waiting on a
            // count that never reaches zero
            if (state.count > 0) {
                state.count--
                if (state.count == 0) {
                    state.idle.signalAll()
                }
            }
        } finally {
            state.lock.unlock()
        }
    }

    /**
     * Refuse every request that has not entered yet
     *
     * Called before a plugin's routes are unregistered, so a request that resolved its handler
     * just before that is refused outright instead of being counted and waited for.
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun beginDrain(pluginId: String) {
        val state = states.computeIfAbsent(pluginId) { State() }

        state.lock.lock()
        try {
            state.draining = true
        } finally {
            state.lock.unlock()
        }
    }

    /**
     * Wait for the requests already inside a plugin to finish
     *
     * @param pluginId Plugin ID
     * @param timeoutMillis How long to wait before giving up
     * @return true when nothing is in flight any more, false when the timeout expired first -
     *         the caller tears the plugin down anyway, since an administrator's operation
     *         cannot be held forever by a request that will not finish
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun awaitDrain(pluginId: String, timeoutMillis: Long): Boolean {
        val state = states[pluginId] ?: return true
        var remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis)

        state.lock.lock()
        try {
            while (state.count > 0) {
                if (remainingNanos <= 0) {
                    return false
                }

                remainingNanos = state.idle.awaitNanos(remainingNanos)
            }

            return true
        } finally {
            state.lock.unlock()
        }
    }

    /**
     * Forget a plugin's state
     *
     * Called once a plugin has been torn down, so its next mount starts from a clean count
     * rather than inheriting the one its previous life left behind.
     *
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun release(pluginId: String) {
        states.remove(pluginId)
    }
}
