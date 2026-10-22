package it.pv.segnapuntipadel.lite.session

/**
 * Cronometro basato su un orologio monotono fornito dal chiamante
 * (es. SystemClock.elapsedRealtime()). Conta solo gli intervalli tra start e stop.
 */
class MatchClock(initialElapsedMs: Long = 0L) {
    private var accumulatedMs: Long
    private var runningSinceMs: Long? = null

    init {
        require(initialElapsedMs >= 0L) { "Tempo trascorso negativo: $initialElapsedMs" }
        accumulatedMs = initialElapsedMs
    }

    val isRunning: Boolean
        get() = runningSinceMs != null

    fun start(nowMs: Long) {
        if (runningSinceMs == null) runningSinceMs = nowMs
    }

    fun stop(nowMs: Long) {
        val since = runningSinceMs ?: return
        accumulatedMs += positiveDelta(since, nowMs)
        runningSinceMs = null
    }

    fun elapsedMs(nowMs: Long): Long {
        val since = runningSinceMs ?: return accumulatedMs
        return accumulatedMs + positiveDelta(since, nowMs)
    }

    private fun positiveDelta(since: Long, now: Long): Long =
        if (now > since) now - since else 0L
}