package it.pv.segnapuntipadel.domain.usecase

import it.pv.segnapuntipadel.domain.model.MatchAction
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.scoring.ScoringEngine

class ApplyMatchAction(
    private val scoringEngine: ScoringEngine = ScoringEngine()
) {
    fun execute(state: MatchState, action: MatchAction): MatchState =
        scoringEngine.apply(state, action)

    fun replay(initialState: MatchState, actions: List<MatchAction>): MatchState =
        actions.fold(initialState, scoringEngine::apply)
}
