package top.fatweb.apimanagement.util

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Run something once the transaction around the caller has committed
 *
 * Outside a transaction the action runs immediately. `afterCommit` runs on the thread
 * that committed, so the action has to be a hand-off rather than the work itself.
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
