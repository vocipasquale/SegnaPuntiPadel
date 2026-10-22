package it.pv.segnapuntipadel.lite.session

import it.pv.segnapuntipadel.domain.model.MatchAction
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.usecase.ApplyMatchAction

/** Dati minimi per ricostruire una partita: configurazione, azioni effettive, tempo. */
data class SavedMatch(
    val config: MatchConfig,
    val actions: List<TeamSide>,
    val elapsedMs: Long
)

sealed class ScoreOutcome {
    object Ignored : ScoreOutcome()
    data class ServiceSelected(val side: TeamSide) : ScoreOutcome()
    object PointScored : ScoreOutcome()
    data class SetCompleted(val setNumber: Int, val winner: TeamSide) : ScoreOutcome()
    data class MatchCompleted(val winner: TeamSide?) : ScoreOutcome()
}

sealed class CorrectionOutcome {
    object NothingToCorrect : CorrectionOutcome()
    data class Corrected(val matchFinished: Boolean) : CorrectionOutcome()
}

/**
 * Sessione di gioco senza dipendenze Android. Il punteggio è sempre calcolato da
 * ApplyMatchAction a partire dalle azioni registrate; la prima azione è la scelta
 * della battuta e non è mai rimossa dalla correzione.
 */
class LiteMatchSession private constructor(
    val config: MatchConfig,
    private val applyMatchAction: ApplyMatchAction,
    private val actionSides: MutableList<TeamSide>,
    initialState: MatchState,
    elapsedMs: Long
) {
    private val startState = MatchState(config)

    val clock = MatchClock(elapsedMs)

    var state: MatchState = initialState
        private set

    val actions: List<TeamSide>
        get() = ArrayList(actionSides)

    fun score(side: TeamSide): ScoreOutcome {
        val previous = state
        val updated = applyMatchAction.execute(previous, MatchAction.ScorePoint(side))
        if (updated == previous) return ScoreOutcome.Ignored

        actionSides.add(side)
        state = updated

        return when {
            updated.completedSets.size > previous.completedSets.size ->
                if (updated.isFinished) {
                    ScoreOutcome.MatchCompleted(updated.winner)
                } else {
                    ScoreOutcome.SetCompleted(
                        updated.completedSets.size,
                        updated.completedSets[updated.completedSets.size - 1].winner
                    )
                }
            previous.servingSide == null -> ScoreOutcome.ServiceSelected(side)
            else -> ScoreOutcome.PointScored
        }
    }

    /** Rimuove l'ultimo punto della squadra indicata (esclusa la scelta iniziale della battuta). */
    fun correctLastPoint(side: TeamSide): CorrectionOutcome {
        var index = -1
        for (i in actionSides.size - 1 downTo FIRST_POINT_INDEX) {
            if (actionSides[i] == side) {
                index = i
                break
            }
        }
        if (index < 0) return CorrectionOutcome.NothingToCorrect

        actionSides.removeAt(index)
        var replayed = startState
        val effective = ArrayList<TeamSide>(actionSides.size)
        for (actionSide in actionSides) {
            val next = applyMatchAction.execute(replayed, MatchAction.ScorePoint(actionSide))
            if (next != replayed) {
                effective.add(actionSide)
                replayed = next
            }
        }
        actionSides.clear()
        actionSides.addAll(effective)
        state = replayed
        return CorrectionOutcome.Corrected(replayed.isFinished)
    }

    fun snapshot(nowMs: Long): SavedMatch =
        SavedMatch(config, actions, clock.elapsedMs(nowMs))

    companion object {
        private const val FIRST_POINT_INDEX = 1

        fun newMatch(
            config: MatchConfig,
            applyMatchAction: ApplyMatchAction = ApplyMatchAction()
        ): LiteMatchSession {
            requireValidRules(config)
            return LiteMatchSession(
                config,
                applyMatchAction,
                ArrayList(),
                MatchState(config),
                0L
            )
        }

        /** @throws IllegalArgumentException se i dati salvati non sono coerenti. */
        fun restore(
            saved: SavedMatch,
            applyMatchAction: ApplyMatchAction = ApplyMatchAction()
        ): LiteMatchSession {
            requireValidRules(saved.config)
            require(saved.elapsedMs >= 0L) { "Tempo trascorso negativo: ${saved.elapsedMs}" }

            var state = MatchState(saved.config)
            saved.actions.forEachIndexed { index, side ->
                val next = applyMatchAction.execute(state, MatchAction.ScorePoint(side))
                require(next != state) { "Azione ${index + 1} non applicabile alla partita salvata" }
                state = next
            }
            return LiteMatchSession(
                saved.config,
                applyMatchAction,
                ArrayList(saved.actions),
                state,
                saved.elapsedMs
            )
        }

        private fun requireValidRules(config: MatchConfig) {
            require(RulesValidator.validate(config.rules) is RulesValidation.Valid) {
                "Regole non valide: ${config.rules}"
            }
        }
    }
}