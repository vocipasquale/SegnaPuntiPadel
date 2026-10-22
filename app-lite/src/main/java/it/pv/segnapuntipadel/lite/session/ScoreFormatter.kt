package it.pv.segnapuntipadel.lite.session

import it.pv.segnapuntipadel.domain.model.GameScore
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.Score
import it.pv.segnapuntipadel.domain.model.TeamSide
import java.util.Locale

/** Formattazione identica a GameActivity dell'app originale. */
object ScoreFormatter {
    fun points(
        gameScore: GameScore,
        side: TeamSide,
        killerLabel: String,
        deuceLabel: (Int) -> String
    ): String {
        if (gameScore.killerPoint) return killerLabel
        val advantage = gameScore.advantage
        if (advantage != null) return if (advantage == side) "1" else "0"
        if (gameScore.completedDeuceCycles > 0) return deuceLabel(gameScore.completedDeuceCycles)

        return when (gameScore.points.forSide(side)) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            else -> "40"
        }
    }

    /** Solo set completati più il set in corso (se la partita non è finita). */
    fun setHistory(state: MatchState): List<Score> {
        val history = ArrayList<Score>(state.completedSets.size + 1)
        state.completedSets.mapTo(history) { it.games }
        if (!state.isFinished) history.add(state.games)
        return history
    }

    fun elapsed(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1_000L
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }
}