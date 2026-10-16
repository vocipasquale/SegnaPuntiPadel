package it.pv.segnapuntipadel.domain.model

import java.io.Serializable

enum class TeamSide : Serializable {
    A,
    B;

    fun other(): TeamSide = if (this == A) B else A
}

data class Team(
    val firstPlayerName: String,
    val secondPlayerName: String
) : Serializable {
    fun displayName(): String {
        val first = firstPlayerName.ifBlank { "Giocatore 1" }
        val second = secondPlayerName.ifBlank { "Giocatore 2" }
        return "$first / $second"
    }
}

data class MatchRules(
    val setsToPlay: Int = DEFAULT_SETS_TO_PLAY,
    val maxGamesPerSet: Int = DEFAULT_MAX_GAMES_PER_SET,
    val maxDeuceCycles: Int = DEFAULT_MAX_DEUCE_CYCLES
) : Serializable {
    companion object {
        const val DEFAULT_SETS_TO_PLAY = 3
        const val DEFAULT_MAX_GAMES_PER_SET = 10
        const val DEFAULT_MAX_DEUCE_CYCLES = 1
    }
}

data class MatchConfig(
    val teamA: Team,
    val teamB: Team,
    val rules: MatchRules
) : Serializable {
    fun team(side: TeamSide): Team = if (side == TeamSide.A) teamA else teamB
}

data class Score(
    val a: Int = 0,
    val b: Int = 0
) : Serializable {
    fun forSide(side: TeamSide): Int = if (side == TeamSide.A) a else b

    fun increment(side: TeamSide): Score = when (side) {
        TeamSide.A -> copy(a = a + 1)
        TeamSide.B -> copy(b = b + 1)
    }

    fun differenceFor(side: TeamSide): Int =
        forSide(side) - forSide(side.other())
}

data class GameScore(
    val points: Score = Score(),
    val advantage: TeamSide? = null,
    val completedDeuceCycles: Int = 0,
    val killerPoint: Boolean = false
) : Serializable

data class SetResult(
    val games: Score,
    val winner: TeamSide
) : Serializable

data class MatchState(
    val config: MatchConfig,
    val currentSetNumber: Int = 1,
    val games: Score = Score(),
    val setsWon: Score = Score(),
    val gameScore: GameScore = GameScore(),
    val servingSide: TeamSide? = null,
    val completedSets: List<SetResult> = emptyList(),
    val isFinished: Boolean = false,
    val winner: TeamSide? = null
) : Serializable

sealed class MatchAction : Serializable {
    data class ScorePoint(val side: TeamSide) : MatchAction()
}
