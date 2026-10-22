package it.pv.segnapuntipadel.lite.session

import it.pv.segnapuntipadel.domain.model.MatchAction
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.Score
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.usecase.ApplyMatchAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteMatchSessionTest {
    private fun config(rules: MatchRules = MatchRules()) =
        MatchConfig(Team("Anna", "Bea"), Team("Carlo", "Dino"), rules)

    private fun LiteMatchSession.winGame(side: TeamSide): ScoreOutcome {
        var outcome: ScoreOutcome = ScoreOutcome.Ignored
        repeat(4) { outcome = score(side) }
        return outcome
    }

    @Test
    fun firstTapSelectsServiceWithoutPoint() {
        val session = LiteMatchSession.newMatch(config())

        assertEquals(ScoreOutcome.ServiceSelected(TeamSide.B), session.score(TeamSide.B))
        assertEquals(TeamSide.B, session.state.servingSide)
        assertEquals(Score(), session.state.gameScore.points)

        assertEquals(ScoreOutcome.PointScored, session.score(TeamSide.A))
        assertEquals(Score(1, 0), session.state.gameScore.points)
    }

    @Test
    fun correctionNeverRemovesServiceSelection() {
        val session = LiteMatchSession.newMatch(config())
        session.score(TeamSide.A)

        assertEquals(CorrectionOutcome.NothingToCorrect, session.correctLastPoint(TeamSide.A))
        assertEquals(TeamSide.A, session.state.servingSide)
        assertEquals(listOf(TeamSide.A), session.actions)
    }

    @Test
    fun correctionRemovesLastPointOfSelectedTeamOnly() {
        val session = LiteMatchSession.newMatch(config())
        session.score(TeamSide.A)
        session.score(TeamSide.A)
        session.score(TeamSide.B)
        session.score(TeamSide.B)

        assertEquals(CorrectionOutcome.Corrected(false), session.correctLastPoint(TeamSide.A))
        assertEquals(Score(0, 2), session.state.gameScore.points)
        assertEquals(listOf(TeamSide.A, TeamSide.B, TeamSide.B), session.actions)
        assertEquals(CorrectionOutcome.NothingToCorrect, session.correctLastPoint(TeamSide.A))
    }

    @Test
    fun correctionMatchesDomainReplay() {
        val session = LiteMatchSession.newMatch(config(MatchRules(2, 6, 1)))
        val sides = listOf(
            TeamSide.A, TeamSide.A, TeamSide.B, TeamSide.A, TeamSide.B, TeamSide.B,
            TeamSide.A, TeamSide.B, TeamSide.A, TeamSide.B, TeamSide.B, TeamSide.A
        )
        sides.forEach { session.score(it) }
        val before = session.actions

        session.correctLastPoint(TeamSide.B)

        val expectedActions = before.toMutableList().apply { removeAt(before.lastIndexOf(TeamSide.B)) }
        val expected = ApplyMatchAction().replay(
            MatchState(session.config),
            expectedActions.map { MatchAction.ScorePoint(it) }
        )
        assertEquals(expected, session.state)
    }

    @Test
    fun setAndMatchCompletionOutcomesWithDraw() {
        val session = LiteMatchSession.newMatch(config(MatchRules(2, 6, 1)))
        session.score(TeamSide.A)

        repeat(5) { assertEquals(ScoreOutcome.PointScored, session.winGame(TeamSide.A)) }
        assertEquals(ScoreOutcome.SetCompleted(1, TeamSide.A), session.winGame(TeamSide.A))

        repeat(5) { session.winGame(TeamSide.B) }
        assertEquals(ScoreOutcome.MatchCompleted(null), session.winGame(TeamSide.B))
        assertTrue(session.state.isFinished)
        assertNull(session.state.winner)

        val actionCount = session.actions.size
        assertEquals(ScoreOutcome.Ignored, session.score(TeamSide.A))
        assertEquals(actionCount, session.actions.size)
    }

    @Test
    fun restoreRebuildsStateAndElapsedTime() {
        val session = LiteMatchSession.newMatch(config())
        session.clock.start(1_000L)
        session.score(TeamSide.B)
        session.winGame(TeamSide.A)
        session.score(TeamSide.B)

        val saved = session.snapshot(61_000L)
        val restored = LiteMatchSession.restore(saved)

        assertEquals(session.state, restored.state)
        assertEquals(session.actions, restored.actions)
        assertEquals(60_000L, restored.clock.elapsedMs(0L))
        assertFalse(restored.clock.isRunning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsActionsAfterMatchEnd() {
        val rules = MatchRules(1, 6, 1)
        val actions = ArrayList<TeamSide>()
        actions.add(TeamSide.A)
        repeat(6 * 4) { actions.add(TeamSide.A) }
        actions.add(TeamSide.B)
        LiteMatchSession.restore(SavedMatch(config(rules), actions, 0L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsInvalidRules() {
        LiteMatchSession.restore(SavedMatch(config(MatchRules(1, 5, 1)), emptyList(), 0L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsNegativeElapsedTime() {
        LiteMatchSession.restore(SavedMatch(config(), emptyList(), -1L))
    }
}