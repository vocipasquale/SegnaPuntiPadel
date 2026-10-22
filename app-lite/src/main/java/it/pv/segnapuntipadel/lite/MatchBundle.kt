package it.pv.segnapuntipadel.lite

import android.os.Bundle
import android.os.SystemClock
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.lite.session.SavedMatch

/**
 * Stato transitorio della partita nel Bundle di onSaveInstanceState (solo valori primitivi).
 * Serve unicamente a sopravvivere a ricreazioni dell'Activity decise dal sistema:
 * nulla viene scritto su memoria persistente (vedi docs/Specifiche-002.md).
 */
object MatchBundle {
    private const val KEY_PRESENT = "match_present"
    private const val KEY_A1 = "match_team_a_1"
    private const val KEY_A2 = "match_team_a_2"
    private const val KEY_B1 = "match_team_b_1"
    private const val KEY_B2 = "match_team_b_2"
    private const val KEY_SETS = "match_sets_to_play"
    private const val KEY_MAX_GAMES = "match_max_games"
    private const val KEY_MAX_DEUCES = "match_max_deuces"
    private const val KEY_ACTIONS = "match_actions"
    private const val KEY_ELAPSED_MS = "match_elapsed_ms"
    private const val KEY_CAPTURED_AT_MS = "match_captured_at_ms"

    private const val SIDE_A: Byte = 0
    private const val SIDE_B: Byte = 1

    fun write(out: Bundle, saved: SavedMatch) {
        val config = saved.config
        out.putBoolean(KEY_PRESENT, true)
        out.putString(KEY_A1, config.teamA.firstPlayerName)
        out.putString(KEY_A2, config.teamA.secondPlayerName)
        out.putString(KEY_B1, config.teamB.firstPlayerName)
        out.putString(KEY_B2, config.teamB.secondPlayerName)
        out.putInt(KEY_SETS, config.rules.setsToPlay)
        out.putInt(KEY_MAX_GAMES, config.rules.maxGamesPerSet)
        out.putInt(KEY_MAX_DEUCES, config.rules.maxDeuceCycles)
        val actions = ByteArray(saved.actions.size)
        saved.actions.forEachIndexed { i, side -> actions[i] = if (side == TeamSide.A) SIDE_A else SIDE_B }
        out.putByteArray(KEY_ACTIONS, actions)
        out.putLong(KEY_ELAPSED_MS, saved.elapsedMs)
        out.putLong(KEY_CAPTURED_AT_MS, SystemClock.elapsedRealtime())
    }

    /**
     * @return null se il Bundle non contiene una partita.
     * @throws IllegalArgumentException se i dati sono incompleti o non validi.
     */
    fun read(bundle: Bundle): SavedMatch? {
        if (!bundle.getBoolean(KEY_PRESENT, false)) return null

        val rawActions = requireNotNull(bundle.getByteArray(KEY_ACTIONS)) { "azioni mancanti" }
        val actions = ArrayList<TeamSide>(rawActions.size)
        rawActions.forEachIndexed { i, value ->
            actions.add(
                when (value) {
                    SIDE_A -> TeamSide.A
                    SIDE_B -> TeamSide.B
                    else -> throw IllegalArgumentException("azione ${i + 1} non valida: $value")
                }
            )
        }
        require(bundle.containsKey(KEY_SETS) && bundle.containsKey(KEY_MAX_GAMES) &&
            bundle.containsKey(KEY_MAX_DEUCES) && bundle.containsKey(KEY_ELAPSED_MS)) {
            "regole o tempo mancanti"
        }
        val config = MatchConfig(
            teamA = Team(requireString(bundle, KEY_A1), requireString(bundle, KEY_A2)),
            teamB = Team(requireString(bundle, KEY_B1), requireString(bundle, KEY_B2)),
            rules = MatchRules(
                setsToPlay = bundle.getInt(KEY_SETS),
                maxGamesPerSet = bundle.getInt(KEY_MAX_GAMES),
                maxDeuceCycles = bundle.getInt(KEY_MAX_DEUCES)
            )
        )
        val elapsedMs = bundle.getLong(KEY_ELAPSED_MS)
        require(elapsedMs >= 0L) { "tempo trascorso negativo" }
        val capturedAtMs = bundle.getLong(KEY_CAPTURED_AT_MS, SystemClock.elapsedRealtime())
        val backgroundMs = maxOf(0L, SystemClock.elapsedRealtime() - capturedAtMs)
        require(elapsedMs <= Long.MAX_VALUE - backgroundMs) { "tempo trascorso non valido" }
        return SavedMatch(config, actions, elapsedMs + backgroundMs)
    }

    private fun requireString(bundle: Bundle, key: String): String =
        requireNotNull(bundle.getString(key)) { "campo $key mancante" }
}