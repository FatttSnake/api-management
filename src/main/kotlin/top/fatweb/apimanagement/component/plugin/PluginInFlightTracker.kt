package top.fatweb.apimanagement.component.plugin

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Plugin in-flight request tracker
 *
 * Counts the requests currently executing inside each plugin, so a teardown can wait for them
 * instead of closing the datasource pools and class loader out from under running code. A
 * request leaves by the handle it was given rather than by its plugin ID.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
@Component
class PluginInFlightTracker {
    /**
     * The admission a request was granted
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
         */
        internal val state: State
    )

    /**
     * Per-plugin admission state
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
     * @param pluginId Plugin ID
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    fun release(pluginId: String) {
        states.remove(pluginId)
    }
}
