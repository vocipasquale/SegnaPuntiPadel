package it.pv.segnapuntipadel.lite.session

import it.pv.segnapuntipadel.domain.model.GameScore
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.Score
import it.pv.segnapuntipadel.domain.model.SetResult
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportTest {
    private val deuce = { count: Int -> "Parità raggiunte: $count" }

    @Test
    fun formatsPointsLikeOriginalApp() {
        assertEquals("0", ScoreFormatter.points(GameScore(), TeamSide.A, "k", deuce))
        assertEquals("15", ScoreFormatter.points(GameScore(Score(1, 2)), TeamSide.A, "k", deuce))
        assertEquals("30", ScoreFormatter.points(GameScore(Score(1, 2)), TeamSide.B, "k", deuce))
        assertEquals("40", ScoreFormatter.points(GameScore(Score(3, 2)), TeamSide.A, "k", deuce))
    }

    @Test
    fun formatsAdvantageDeuceAndKillerPoint() {
        val advantageA = GameScore(Score(3, 3), advantage = TeamSide.A)
        assertEquals("1", ScoreFormatter.points(advantageA, TeamSide.A, "k", deuce))
        assertEquals("0", ScoreFormatter.points(advantageA, TeamSide.B, "k", deuce))

        val deuceTwice = GameScore(Score(3, 3), completedDeuceCycles = 2)
        assertEquals("Parità raggiunte: 2", ScoreFormatter.points(deuceTwice, TeamSide.B, "k", deuce))

        val killer = GameScore(Score(3, 3), completedDeuceCycles = 1, killerPoint = true)
        assertEquals("k", ScoreFormatter.points(killer, TeamSide.A, "k", deuce))
    }

    @Test
    fun historyContainsOnlyCompletedAndCurrentSet() {
        val config = MatchConfig(Team("", ""), Team("", ""), MatchRules(1000, 6, 1))
        val inProgress = MatchState(
            config,
            games = Score(2, 1),
            completedSets = listOf(SetResult(Score(6, 4), TeamSide.A))
        )
        assertEquals(listOf(Score(6, 4), Score(2, 1)), ScoreFormatter.setHistory(inProgress))

        val finished = inProgress.copy(isFinished = true, games = Score())
        assertEquals(listOf(Score(6, 4)), ScoreFormatter.setHistory(finished))
    }

    @Test
    fun formatsElapsedTime() {
        assertEquals("00:00", ScoreFormatter.elapsed(999L))
        assertEquals("01:05", ScoreFormatter.elapsed(65_000L))
        assertEquals("125:00", ScoreFormatter.elapsed(7_500_000L))
    }

    @Test
    fun clockCountsOnlyRunningIntervals() {
        val clock = MatchClock(5_000L)
        assertEquals(5_000L, clock.elapsedMs(100L))
        clock.start(1_000L)
        assertTrue(clock.isRunning)
        assertEquals(7_000L, clock.elapsedMs(3_000L))
        clock.stop(4_000L)
        assertFalse(clock.isRunning)
        assertEquals(8_000L, clock.elapsedMs(99_000L))
        clock.start(10_000L)
        assertEquals(8_000L, clock.elapsedMs(9_000L))
    }

    @Test
    fun validatesRulesLikeOriginalSetup() {
        assertEquals(RulesValidation.Valid(MatchRules(3, 10, 1)), RulesValidator.validate(3, 10, 1))
        assertEquals(RulesValidation.InvalidNumber, RulesValidator.validate(null, 10, 1))
        assertEquals(RulesValidation.NonPositive, RulesValidator.validate(0, 10, 1))
        assertEquals(RulesValidation.NonPositive, RulesValidator.validate(3, 10, 0))
        assertEquals(RulesValidation.MaxGamesTooLow, RulesValidator.validate(3, 5, 1))
        assertEquals(RulesValidation.Valid(MatchRules(99, 40, 7)), RulesValidator.validate(99, 40, 7))
        assertEquals(null, RulesValidator.parse("abc"))
        assertEquals(12, RulesValidator.parse(" 12 "))
    }
}