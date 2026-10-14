package com.example.segnapuntipadel.domain.scoring

import com.example.segnapuntipadel.domain.model.GameScore
import com.example.segnapuntipadel.domain.model.MatchAction
import com.example.segnapuntipadel.domain.model.MatchState
import com.example.segnapuntipadel.domain.model.Score
import com.example.segnapuntipadel.domain.model.SetResult
import com.example.segnapuntipadel.domain.model.TeamSide

class ScoringEngine {
    fun apply(state: MatchState, action: MatchAction): MatchState {
        if (state.isFinished) return state

        return when (action) {
            is MatchAction.ScorePoint -> scorePoint(state, action.side)
        }
    }

    private fun scorePoint(state: MatchState, side: TeamSide): MatchState {
        val gameScore = state.gameScore

        if (gameScore.killerPoint) {
            return awardGame(state, side)
        }

        val advantage = gameScore.advantage
        if (advantage != null) {
            return if (advantage == side) {
                awardGame(state, side)
            } else {
                val completedCycles = gameScore.completedDeuceCycles + 1
                state.copy(
                    gameScore = GameScore(
                        points = gameScore.points,
                        completedDeuceCycles = completedCycles,
                        killerPoint = completedCycles >= state.config.rules.maxDeuceCycles
                    )
                )
            }
        }

        val points = gameScore.points
        val ownPoints = points.forSide(side)
        val opponentPoints = points.forSide(side.other())

        if (ownPoints == 3 && opponentPoints < 3) {
            return awardGame(state, side)
        }

        if (ownPoints == 3 && opponentPoints == 3) {
            return state.copy(gameScore = gameScore.copy(advantage = side))
        }

        return state.copy(gameScore = gameScore.copy(points = points.increment(side)))
    }

    private fun awardGame(state: MatchState, side: TeamSide): MatchState {
        val games = state.games.increment(side)
        if (!isSetComplete(games, state.config.rules.maxGamesPerSet)) {
            return state.copy(games = games, gameScore = GameScore())
        }

        val setWinner = if (games.a > games.b) TeamSide.A else TeamSide.B
        val completedSets = state.completedSets + SetResult(games, setWinner)
        val setsWon = state.setsWon.increment(setWinner)
        val isFinished = completedSets.size >= state.config.rules.setsToPlay
        val winner = if (isFinished) {
            when {
                setsWon.a > setsWon.b -> TeamSide.A
                setsWon.b > setsWon.a -> TeamSide.B
                else -> null
            }
        } else {
            null
        }

        return state.copy(
            currentSetNumber = if (isFinished) state.currentSetNumber else state.currentSetNumber + 1,
            games = Score(),
            setsWon = setsWon,
            gameScore = GameScore(),
            completedSets = completedSets,
            isFinished = isFinished,
            winner = winner
        )
    }

    private fun isSetComplete(games: Score, maxGames: Int): Boolean {
        val aReachedLimit = games.a >= maxGames
        val bReachedLimit = games.b >= maxGames
        if (aReachedLimit || bReachedLimit) return true

        return (games.a >= 6 && games.differenceFor(TeamSide.A) >= 2) ||
            (games.b >= 6 && games.differenceFor(TeamSide.B) >= 2)
    }
}
