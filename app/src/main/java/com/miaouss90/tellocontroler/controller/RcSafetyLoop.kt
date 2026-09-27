package com.miaouss90.tellocontroler.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * SAFETY-CRITICAL. The only component allowed to emit RC commands.
 *
 * Sends the latest input at a fixed rate; if no fresh input arrived within [STALE_MS],
 * sends neutral instead, so a lost controller event can never leave a non-zero command active.
 */
class RcSafetyLoop(
    private val send: (RcInput) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        const val PERIOD_MS = 50L
        const val STALE_MS = 250L
    }

    private data class Timed(val input: RcInput, val at: Long)

    private val latest = AtomicReference(Timed(RcInput.NEUTRAL, 0))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            while (isActive) {
                send(commandAt(clock()))
                delay(PERIOD_MS)
            }
        }
    }

    fun update(input: RcInput) = latest.set(Timed(input, clock()))

    fun neutral() = latest.set(Timed(RcInput.NEUTRAL, clock()))

    /** The command the loop would send at time [now]. */
    fun commandAt(now: Long): RcInput {
        val v = latest.get()
        return if (now - v.at <= STALE_MS) v.input else RcInput.NEUTRAL
    }

    fun stop() {
        neutral()
        send(RcInput.NEUTRAL)
        scope.cancel()
    }
}
