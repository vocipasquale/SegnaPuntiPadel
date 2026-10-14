package com.example.segnapuntipadel.domain.usecase

import com.example.segnapuntipadel.domain.model.MatchAction
import com.example.segnapuntipadel.domain.model.MatchState
import com.example.segnapuntipadel.domain.scoring.ScoringEngine

class ApplyMatchAction(
    private val scoringEngine: ScoringEngine = ScoringEngine()
) {
    fun execute(state: MatchState, action: MatchAction): MatchState =
        scoringEngine.apply(state, action)

    fun replay(initialState: MatchState, actions: List<MatchAction>): MatchState =
        actions.fold(initialState, scoringEngine::apply)
}
