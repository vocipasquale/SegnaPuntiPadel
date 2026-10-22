package it.pv.segnapuntipadel

import it.pv.segnapuntipadel.domain.model.MatchAction
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.Score
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.scoring.ScoringEngine
import it.pv.segnapuntipadel.domain.usecase.ApplyMatchAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {
    private val engine = ScoringEngine()

    @Test
    fun repeatedDeuceTurnsIntoKillerPointAtConfiguredLimit() {
        var state = newState(MatchRules(maxDeuceCycles = 1))
        state = score(state, TeamSide.A, 3)
        state = score(state, TeamSide.B, 3)

        state = score(state, TeamSide.A)
        assertEquals(TeamSide.A, state.gameScore.advantage)

        state = score(state, TeamSide.B)
        assertTrue(state.gameScore.killerPoint)
        assertEquals(1, state.gameScore.completedDeuceCycles)

        state = score(state, TeamSide.B)
        assertEquals(ScoreValue(0, 1), ScoreValue(state.games.a, state.games.b))
    }

    @Test
    fun firstScoreInputSelectsServingTeamWithoutAwardingPoint() {
        val state = engine.apply(unstartedState(), MatchAction.ScorePoint(TeamSide.B))

        assertEquals(TeamSide.B, state.servingSide)
        assertEquals(Score(), state.gameScore.points)
        assertNull(state.gameScore.advantage)
    }

    @Test
    fun servingTeamAlternatesAfterEveryGame() {
        var state = winGame(newState(), TeamSide.A)

        assertEquals(1, state.games.a)
        assertEquals(TeamSide.B, state.servingSide)

        state = winGame(state, TeamSide.B)

        assertEquals(1, state.games.b)
        assertEquals(TeamSide.A, state.servingSide)
    }

    @Test
    fun setCanContinuePastSixGamesUntilTwoGameLead() {
        var state = newState()
        repeat(5) {
            state = winGame(state, TeamSide.A)
            state = winGame(state, TeamSide.B)
        }

        state = winGame(state, TeamSide.A)
        assertEquals(6, state.games.a)
        assertEquals(5, state.games.b)
        assertTrue(state.completedSets.isEmpty())

        state = winGame(state, TeamSide.A)
        assertEquals(7, state.completedSets.single().games.a)
        assertEquals(5, state.completedSets.single().games.b)
        assertEquals(1, state.setsWon.a)
    }

    @Test
    fun reachingConfiguredGameLimitEndsSetEvenWithoutTwoGameLead() {
        var state = newState(MatchRules(maxGamesPerSet = 10))
        repeat(9) {
            state = winGame(state, TeamSide.A)
            state = winGame(state, TeamSide.B)
        }

        state = winGame(state, TeamSide.B)
        assertEquals(9, state.completedSets.single().games.a)
        assertEquals(10, state.completedSets.single().games.b)
        assertEquals(TeamSide.B, state.completedSets.single().winner)
    }

    @Test
    fun allConfiguredSetsArePlayedBeforeMatchEnds() {
        val rules = MatchRules(setsToPlay = 3, maxGamesPerSet = 6)
        var state = newState(rules)
        state = winSet(state, TeamSide.A)
        assertFalse(state.isFinished)
        state = winSet(state, TeamSide.B)
        assertFalse(state.isFinished)
        state = winSet(state, TeamSide.B)

        assertTrue(state.isFinished)
        assertEquals(TeamSide.B, state.winner)
        assertEquals(3, state.completedSets.size)
    }

    @Test
    fun evenSetCountCanFinishInADraw() {
        val rules = MatchRules(setsToPlay = 2, maxGamesPerSet = 6)
        var state = newState(rules)
        state = winSet(state, TeamSide.A)
        state = winSet(state, TeamSide.B)

        assertTrue(state.isFinished)
        assertNull(state.winner)
    }

    @Test
    fun finishedMatchIgnoresFurtherPoints() {
        val finished = winSet(newState(MatchRules(setsToPlay = 1)), TeamSide.A)

        assertEquals(finished, engine.apply(finished, MatchAction.ScorePoint(TeamSide.B)))
    }

    @Test
    fun advantageWinnerClosesGameAndResetsDeuceState() {
        var state = score(score(newState(), TeamSide.A, 3), TeamSide.B, 3)
        state = score(state, TeamSide.A, 2)

        assertEquals(Score(1, 0), state.games)
        assertEquals(Score(), state.gameScore.points)
        assertNull(state.gameScore.advantage)
        assertEquals(0, state.gameScore.completedDeuceCycles)
        assertFalse(state.gameScore.killerPoint)
    }

    @Test
    fun killerPointWaitsForConfiguredNumberOfDeuceCycles() {
        var state = score(
            score(newState(MatchRules(maxDeuceCycles = 2)), TeamSide.A, 3),
            TeamSide.B, 3
        )
        state = score(score(state, TeamSide.A), TeamSide.B)
        assertFalse(state.gameScore.killerPoint)
        assertEquals(1, state.gameScore.completedDeuceCycles)

        state = score(score(state, TeamSide.B), TeamSide.A)
        assertTrue(state.gameScore.killerPoint)
        assertEquals(2, state.gameScore.completedDeuceCycles)
    }

    @Test
    fun replayWithoutLastWinningPointReopensFinishedMatch() {
        val initial = unstartedState(MatchRules(setsToPlay = 1))
        val actions = List(25) { MatchAction.ScorePoint(TeamSide.A) }
        val useCase = ApplyMatchAction()
        assertTrue(useCase.replay(initial, actions).isFinished)

        val corrected = useCase.replay(initial, actions.dropLast(1))

        assertFalse(corrected.isFinished)
        assertEquals(Score(5, 0), corrected.games)
        assertEquals(Score(3, 0), corrected.gameScore.points)
        assertEquals(TeamSide.B, corrected.servingSide)
        assertTrue(corrected.completedSets.isEmpty())
    }

    @Test
    fun replayPreservesInitialServingSelectionWhenPointIsRemoved() {
        val initial = unstartedState()
        val actions = listOf(
            MatchAction.ScorePoint(TeamSide.B),
            MatchAction.ScorePoint(TeamSide.A),
            MatchAction.ScorePoint(TeamSide.B)
        )
        val corrected = ApplyMatchAction().replay(initial, listOf(actions[0], actions[2]))

        assertEquals(TeamSide.B, corrected.servingSide)
        assertEquals(Score(0, 1), corrected.gameScore.points)
    }

    private fun unstartedState(rules: MatchRules = MatchRules()): MatchState =
        MatchState(
            MatchConfig(
                teamA = Team("A1", "A2"),
                teamB = Team("B1", "B2"),
                rules = rules
            )
        )

    private fun newState(rules: MatchRules = MatchRules()): MatchState =
        engine.apply(unstartedState(rules), MatchAction.ScorePoint(TeamSide.A))

    private fun score(
        state: MatchState,
        side: TeamSide,
        count: Int = 1
    ): MatchState {
        var result = state
        repeat(count) {
            result = engine.apply(result, MatchAction.ScorePoint(side))
        }
        return result
    }

    private fun winGame(state: MatchState, side: TeamSide): MatchState =
        score(state, side, 4)

    private fun winSet(state: MatchState, side: TeamSide): MatchState {
        var result = state
        repeat(6) {
            result = winGame(result, side)
        }
        return result
    }

    private data class ScoreValue(val a: Int, val b: Int)
}
