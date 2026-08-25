package top.fatweb.apimanagement.util

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Run something once the transaction around the caller has committed
 *
 * A change to what a cache reflects is only worth acting on once it is stored: acting inside
 * the transaction lets a concurrent reader repopulate from the values that are about to be
 * rolled back. `afterCommit` runs on the thread that committed, which is the request thread,
 * so what it is given has to be a hand-off rather than the work itself. That thread still has
 * the transaction's connection bound to it, which is worth leaving alone: a write made here
 * would ride a connection whose transaction is already over.
 *
 * Outside a transaction the action runs immediately, because there is nothing to wait for.
 *
 * @param action Action to run once the surrounding transaction has committed
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun onCommitted(action: () -> Unit) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        action()

        return
    }

    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
        override fun afterCommit() = action()
    })
}
