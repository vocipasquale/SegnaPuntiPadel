package it.pv.segnapuntipadel.domain.remote

import it.pv.segnapuntipadel.domain.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteKeyMapperTest {
    private val actionUp = 1
    private val actionMultiple = 2

    @Test
    fun volumeUpScoresForTeamAAndVolumeDownForTeamB() {
        assertEquals(
            RemoteKeyDecision.ScorePoint(TeamSide.A),
            RemoteKeyMapper.decide(RemoteKeyMapper.KEYCODE_VOLUME_UP, RemoteKeyMapper.ACTION_DOWN, 0, true)
        )
        assertEquals(
            RemoteKeyDecision.ScorePoint(TeamSide.B),
            RemoteKeyMapper.decide(RemoteKeyMapper.KEYCODE_VOLUME_DOWN, RemoteKeyMapper.ACTION_DOWN, 0, true)
        )
    }

    /** Sequenza reale registrata dall'AB Shutter3 tenendo premuto VOLUME_UP (13 ripetizioni). */
    @Test
    fun longPressAssignsExactlyOnePoint() {
        val key = RemoteKeyMapper.KEYCODE_VOLUME_UP
        val events = listOf(RemoteKeyMapper.ACTION_DOWN to 0) +
            (1..13).map { RemoteKeyMapper.ACTION_DOWN to it } +
            listOf(actionUp to 0)

        val decisions = events.map { (action, repeat) -> RemoteKeyMapper.decide(key, action, repeat, true) }

        assertEquals(1, decisions.count { it is RemoteKeyDecision.ScorePoint })
        assertEquals(RemoteKeyDecision.ScorePoint(TeamSide.A), decisions.first())
        assertEquals(events.size - 1, decisions.count { it == RemoteKeyDecision.Ignore })
    }

    @Test
    fun consecutiveShortPressesScoreEachTime() {
        val key = RemoteKeyMapper.KEYCODE_VOLUME_DOWN
        val decisions = List(3) {
            listOf(
                RemoteKeyMapper.decide(key, RemoteKeyMapper.ACTION_DOWN, 0, true),
                RemoteKeyMapper.decide(key, actionUp, 0, true)
            )
        }.flatten()

        assertEquals(3, decisions.count { it == RemoteKeyDecision.ScorePoint(TeamSide.B) })
    }

    @Test
    fun releaseAndMultipleEventsAreConsumedWithoutPoints() {
        val key = RemoteKeyMapper.KEYCODE_VOLUME_UP
        assertEquals(RemoteKeyDecision.Ignore, RemoteKeyMapper.decide(key, actionUp, 0, true))
        assertEquals(RemoteKeyDecision.Ignore, RemoteKeyMapper.decide(key, actionMultiple, 3, true))
    }

    @Test
    fun builtInVolumeKeysAreLeftToTheSystem() {
        for (key in listOf(RemoteKeyMapper.KEYCODE_VOLUME_UP, RemoteKeyMapper.KEYCODE_VOLUME_DOWN)) {
            assertEquals(
                RemoteKeyDecision.PassThrough,
                RemoteKeyMapper.decide(key, RemoteKeyMapper.ACTION_DOWN, 0, false)
            )
            assertEquals(RemoteKeyDecision.PassThrough, RemoteKeyMapper.decide(key, actionUp, 0, false))
        }
    }

    @Test
    fun otherKeysFromRemoteAreLeftToTheSystem() {
        val keyBack = 4
        val keyEnter = 66
        assertEquals(RemoteKeyDecision.PassThrough, RemoteKeyMapper.decide(keyBack, RemoteKeyMapper.ACTION_DOWN, 0, true))
        assertEquals(RemoteKeyDecision.PassThrough, RemoteKeyMapper.decide(keyEnter, RemoteKeyMapper.ACTION_DOWN, 0, true))
    }
}
