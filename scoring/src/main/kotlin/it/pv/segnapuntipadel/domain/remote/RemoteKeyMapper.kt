package it.pv.segnapuntipadel.domain.remote

import it.pv.segnapuntipadel.domain.model.TeamSide

/** Esito di un evento tasto ricevuto durante la partita. */
sealed class RemoteKeyDecision {
    /** Nuova pressione di un tasto del telecomando: assegna un punto alla squadra. */
    data class ScorePoint(val side: TeamSide) : RemoteKeyDecision()

    /** Evento del telecomando da consumare senza effetti (ripetizioni, rilascio). */
    object Ignore : RemoteKeyDecision()

    /** Evento non gestito: va lasciato al sistema (es. tasti volume del telefono). */
    object PassThrough : RemoteKeyDecision()
}

/**
 * Traduce gli eventi del telecomando Bluetooth (es. AB Shutter3) in punti.
 *
 * Il telecomando invia VOLUME_UP e VOLUME_DOWN; tenendo premuto un tasto Android genera
 * ripetizioni (repeatCount > 0) ogni ~50 ms fino al rilascio. Ogni pressione deve
 * assegnare un solo punto, quindi conta solo il primo ACTION_DOWN.
 *
 * Classe pura JVM: i valori numerici coincidono con le costanti di android.view.KeyEvent.
 */
object RemoteKeyMapper {
    const val KEYCODE_VOLUME_UP = 24
    const val KEYCODE_VOLUME_DOWN = 25
    const val ACTION_DOWN = 0

    fun sideFor(keyCode: Int): TeamSide? = when (keyCode) {
        KEYCODE_VOLUME_UP -> TeamSide.A
        KEYCODE_VOLUME_DOWN -> TeamSide.B
        else -> null
    }

    /**
     * @param fromExternalDevice true solo per dispositivi esterni (telecomando Bluetooth):
     * i tasti volume del telefono devono continuare a regolare il volume.
     */
    fun decide(
        keyCode: Int,
        action: Int,
        repeatCount: Int,
        fromExternalDevice: Boolean
    ): RemoteKeyDecision {
        if (!fromExternalDevice) return RemoteKeyDecision.PassThrough
        val side = sideFor(keyCode) ?: return RemoteKeyDecision.PassThrough
        return if (action == ACTION_DOWN && repeatCount == 0) {
            RemoteKeyDecision.ScorePoint(side)
        } else {
            RemoteKeyDecision.Ignore
        }
    }
}
