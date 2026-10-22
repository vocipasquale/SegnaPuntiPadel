package it.pv.segnapuntipadel.lite

import android.app.Activity
import android.app.AlertDialog
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.Toast
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.remote.RemoteKeyDecision
import it.pv.segnapuntipadel.domain.remote.RemoteKeyMapper
import it.pv.segnapuntipadel.lite.session.CorrectionOutcome
import it.pv.segnapuntipadel.lite.session.LiteMatchSession
import it.pv.segnapuntipadel.lite.session.RulesValidation
import it.pv.segnapuntipadel.lite.session.RulesValidator
import it.pv.segnapuntipadel.lite.session.ScoreFormatter
import it.pv.segnapuntipadel.lite.session.ScoreOutcome
import java.util.ArrayDeque

class MainActivity : Activity() {
    private enum class Screen { SETUP, SCOREBOARD }

    private class BoardViews(
        val setLabel: TextView,
        val timer: TextView,
        val statusHint: TextView,
        val historyScroll: HorizontalScrollView,
        val history: TextView,
        val panelA: View,
        val panelB: View,
        val pointsA: TextView,
        val pointsB: TextView,
        val servingA: View,
        val servingB: View
    )

    private val handler = Handler(Looper.getMainLooper())

    private var session: LiteMatchSession? = null
    private var screen = Screen.SETUP
    private var isInForeground = false
    private var board: BoardViews? = null

    private var finishDialog: AlertDialog? = null
    private var leaveDialog: AlertDialog? = null
    private var finishDialogClosedByUser = false

    private var toneGenerator: ToneGenerator? = null
    private val soundQueue = ArrayDeque<Int>()
    private var remainingBeeps = 0
    private var isPlayingBeepSequence = false
    private val beepSequenceRunner = Runnable {
        remainingBeeps--
        when {
            remainingBeeps > 0 -> playNextBeep()
            !soundQueue.isEmpty() -> {
                remainingBeeps = soundQueue.removeFirst()
                playNextBeep()
            }
            else -> isPlayingBeepSequence = false
        }
    }

    private val tickRunner = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createToneGenerator()

        if (savedInstanceState != null) restoreTransientMatch(savedInstanceState)

        finishDialogClosedByUser = savedInstanceState?.getBoolean(STATE_FINISH_DIALOG_CLOSED) ?: false
        val requestedScreen = savedInstanceState?.getString(STATE_SCREEN)
        if (requestedScreen == Screen.SCOREBOARD.name && session != null) {
            showScoreboard()
        } else {
            showSetup()
        }
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
        if (screen == Screen.SCOREBOARD) enterScoreboardForeground()
    }

    override fun onPause() {
        isInForeground = false
        handler.removeCallbacks(tickRunner)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SCREEN, screen.name)
        outState.putBoolean(STATE_FINISH_DIALOG_CLOSED, finishDialogClosedByUser)
        session?.let { MatchBundle.write(outState, it.snapshot(now())) }
    }

    /** Ripristino solo in memoria (ricreazione decisa dal sistema); nessuna lettura da disco. */
    private fun restoreTransientMatch(savedInstanceState: Bundle) {
        try {
            val saved = MatchBundle.read(savedInstanceState) ?: return
            session = LiteMatchSession.restore(saved)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Stato temporaneo della partita non valido", e)
            Toast.makeText(
                this,
                getString(R.string.restore_failed, e.message.orEmpty()),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        soundQueue.clear()
        isPlayingBeepSequence = false
        dismissDialogs()
        toneGenerator?.let {
            it.stopTone()
            it.release()
        }
        toneGenerator = null
        super.onDestroy()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (screen == Screen.SCOREBOARD) {
            requestLeaveScoreboard()
        } else {
            super.onBackPressed()
        }
    }

    // Telecomando Bluetooth sul tabellone: un punto per pressione, ripetizioni ignorate;
    // i tasti volume del dispositivo non sono intercettati.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (screen != Screen.SCOREBOARD || session == null) return super.dispatchKeyEvent(event)

        val decision = RemoteKeyMapper.decide(
            event.keyCode,
            event.action,
            event.repeatCount,
            ExternalInputDevice.isExternal(event)
        )
        return when (decision) {
            is RemoteKeyDecision.ScorePoint -> {
                onScoreTap(decision.side)
                true
            }
            RemoteKeyDecision.Ignore -> true
            RemoteKeyDecision.PassThrough -> super.dispatchKeyEvent(event)
        }
    }

    // ---------- Setup ----------

    private fun showSetup() {
        handler.removeCallbacks(tickRunner)
        val previousConfig = session?.config
        session = null
        dismissDialogs()
        screen = Screen.SETUP
        board = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_setup)

        val playerA1 = findViewById<EditText>(R.id.player_a1)
        val playerA2 = findViewById<EditText>(R.id.player_a2)
        val playerB1 = findViewById<EditText>(R.id.player_b1)
        val playerB2 = findViewById<EditText>(R.id.player_b2)
        val setsToPlay = findViewById<EditText>(R.id.sets_to_play)
        val maxGames = findViewById<EditText>(R.id.max_games_per_set)
        val maxDeuces = findViewById<EditText>(R.id.max_deuce_cycles)

        val rules = previousConfig?.rules ?: MatchRules()
        playerA1.setText(previousConfig?.teamA?.firstPlayerName.orEmpty())
        playerA2.setText(previousConfig?.teamA?.secondPlayerName.orEmpty())
        playerB1.setText(previousConfig?.teamB?.firstPlayerName.orEmpty())
        playerB2.setText(previousConfig?.teamB?.secondPlayerName.orEmpty())
        setsToPlay.setText(rules.setsToPlay.toString())
        maxGames.setText(rules.maxGamesPerSet.toString())
        maxDeuces.setText(rules.maxDeuceCycles.toString())

        findViewById<Button>(R.id.start_new_match).setOnClickListener {
            val validation = RulesValidator.validate(
                RulesValidator.parse(setsToPlay.text.toString()),
                RulesValidator.parse(maxGames.text.toString()),
                RulesValidator.parse(maxDeuces.text.toString())
            )
            val validRules = when (validation) {
                is RulesValidation.Valid -> validation.rules
                RulesValidation.InvalidNumber -> return@setOnClickListener showMessage(R.string.invalid_number)
                RulesValidation.NonPositive -> return@setOnClickListener showMessage(R.string.invalid_positive_values)
                RulesValidation.MaxGamesTooLow -> return@setOnClickListener showMessage(R.string.invalid_max_games)
            }
            val config = MatchConfig(
                teamA = Team(playerA1.text.toString().trim(), playerA2.text.toString().trim()),
                teamB = Team(playerB1.text.toString().trim(), playerB2.text.toString().trim()),
                rules = validRules
            )
            startNewMatch(config)
        }
    }

    private fun startNewMatch(config: MatchConfig) {
        handler.removeCallbacks(tickRunner)
        val newSession = LiteMatchSession.newMatch(config)
        session = newSession
        finishDialogClosedByUser = false
        showScoreboard()
    }

    // ---------- Tabellone ----------

    private fun showScoreboard() {
        val current = session ?: return showSetup()
        dismissDialogs()
        screen = Screen.SCOREBOARD
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_scoreboard)

        val views = BoardViews(
            setLabel = findViewById<TextView>(R.id.set_label),
            timer = findViewById<TextView>(R.id.match_timer),
            statusHint = findViewById<TextView>(R.id.status_hint),
            historyScroll = findViewById<HorizontalScrollView>(R.id.set_history_scroll),
            history = findViewById<TextView>(R.id.set_history),
            panelA = findViewById<View>(R.id.panel_a),
            panelB = findViewById<View>(R.id.panel_b),
            pointsA = findViewById<TextView>(R.id.team_a_points),
            pointsB = findViewById<TextView>(R.id.team_b_points),
            servingA = findViewById<View>(R.id.team_a_serving_indicator),
            servingB = findViewById<View>(R.id.team_b_serving_indicator)
        )
        board = views

        val nameA = current.config.teamA.displayName()
        val nameB = current.config.teamB.displayName()
        findViewById<TextView>(R.id.team_a_name).text = nameA
        findViewById<TextView>(R.id.team_b_name).text = nameB
        views.panelA.contentDescription = getString(R.string.score_label, nameA)
        views.panelB.contentDescription = getString(R.string.score_label, nameB)

        views.panelA.setOnClickListener { onScoreTap(TeamSide.A) }
        views.panelB.setOnClickListener { onScoreTap(TeamSide.B) }
        views.panelA.setOnLongClickListener {
            onCorrection(TeamSide.A)
            true
        }
        views.panelB.setOnLongClickListener {
            onCorrection(TeamSide.B)
            true
        }

        render(current)
        if (isInForeground) enterScoreboardForeground()
    }

    private fun enterScoreboardForeground() {
        val current = session ?: return
        val nowMs = now()
        current.clock.start(nowMs)
        handler.removeCallbacks(tickRunner)
        handler.post(tickRunner)
        maybeShowFinishDialog()
    }

    private fun onTick() {
        val current = session ?: return
        val views = board ?: return
        views.timer.text = ScoreFormatter.elapsed(current.clock.elapsedMs(now()))
    }

    private fun onScoreTap(side: TeamSide) {
        val current = session ?: return
        if (current.state.isFinished) {
            finishDialogClosedByUser = false
            maybeShowFinishDialog()
            return
        }
        val outcome = current.score(side)
        when (outcome) {
            ScoreOutcome.Ignored -> return
            is ScoreOutcome.ServiceSelected, ScoreOutcome.PointScored -> playBeepSequence(1)
            is ScoreOutcome.SetCompleted -> {
                playBeepSequence(2)
                Toast.makeText(
                    this,
                    getString(
                        R.string.set_won,
                        outcome.setNumber,
                        current.config.team(outcome.winner).displayName()
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
            is ScoreOutcome.MatchCompleted -> {
                playBeepSequence(3)
            }
        }
        render(current)
        if (outcome is ScoreOutcome.MatchCompleted) maybeShowFinishDialog()
    }

    private fun onCorrection(side: TeamSide) {
        val current = session ?: return
        val teamName = getString(if (side == TeamSide.A) R.string.team_a else R.string.team_b)
        when (val outcome = current.correctLastPoint(side)) {
            CorrectionOutcome.NothingToCorrect -> showMessage(getString(R.string.no_point_to_correct, teamName))
            is CorrectionOutcome.Corrected -> {
                showMessage(getString(R.string.point_corrected, teamName))
                if (!outcome.matchFinished) {
                    finishDialogClosedByUser = false
                }
                render(current)
            }
        }
    }

    private fun render(current: LiteMatchSession) {
        val views = board ?: return
        val state = current.state
        val rules = current.config.rules

        views.setLabel.text = getString(R.string.set_label, state.currentSetNumber, rules.setsToPlay)
        views.timer.text = ScoreFormatter.elapsed(current.clock.elapsedMs(now()))
        views.statusHint.text = when {
            state.isFinished -> getString(R.string.match_over_hint)
            state.servingSide == null -> getString(R.string.choose_service)
            else -> ""
        }

        val history = SpannableStringBuilder()
        val sets = ScoreFormatter.setHistory(state)
        for (index in sets.indices) {
            if (index > 0) history.append("  ")
            val start = history.length
            history.append("${sets[index].a}-${sets[index].b}")
            if (!state.isFinished && index == sets.size - 1) {
                history.setSpan(
                    ForegroundColorSpan(resources.getColor(R.color.yellow_led)),
                    start,
                    history.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        views.history.text = history
        views.historyScroll.post { views.historyScroll.fullScroll(View.FOCUS_RIGHT) }

        val killer = getString(R.string.killer_point)
        val deuce = { count: Int -> getString(R.string.deuce_count, count) }
        views.pointsA.text = ScoreFormatter.points(state.gameScore, TeamSide.A, killer, deuce)
        views.pointsB.text = ScoreFormatter.points(state.gameScore, TeamSide.B, killer, deuce)
        views.servingA.visibility = if (state.servingSide == TeamSide.A) View.VISIBLE else View.INVISIBLE
        views.servingB.visibility = if (state.servingSide == TeamSide.B) View.VISIBLE else View.INVISIBLE
    }

    private fun maybeShowFinishDialog() {
        val current = session ?: return
        if (screen != Screen.SCOREBOARD || !current.state.isFinished) return
        if (finishDialog != null || finishDialogClosedByUser || isFinishing) return

        val winner = current.state.winner
        val message = if (winner != null) {
            getString(R.string.match_won, current.config.team(winner).displayName())
        } else {
            getString(R.string.match_draw)
        }
        val dialog = AlertDialog.Builder(this)
            .setMessage(message + "\n\n" + getString(R.string.finish_dialog_hint))
            .setCancelable(true)
            .setOnCancelListener { finishDialogClosedByUser = true }
            .setPositiveButton(R.string.new_match) { _, _ -> startNewMatch(current.config) }
            .setNeutralButton(R.string.new_configuration) { _, _ -> showSetup() }
            .setNegativeButton(R.string.exit) { _, _ -> finish() }
            .create()
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { if (finishDialog === dialog) finishDialog = null }
        finishDialog = dialog
        dialog.show()
    }

    private fun requestLeaveScoreboard() {
        val current = session
        if (current == null || current.state.isFinished || current.actions.isEmpty()) {
            showSetup()
            return
        }
        if (leaveDialog != null) return
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.leave_title)
            .setMessage(R.string.leave_message)
            .setPositiveButton(R.string.leave_confirm) { _, _ -> showSetup() }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnDismissListener { if (leaveDialog === dialog) leaveDialog = null }
        leaveDialog = dialog
        dialog.show()
    }

    private fun dismissDialogs() {
        finishDialog?.let {
            finishDialog = null
            it.dismiss()
        }
        leaveDialog?.let {
            leaveDialog = null
            it.dismiss()
        }
    }

    // ---------- Suoni ----------

    private fun createToneGenerator() {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
        } catch (e: RuntimeException) {
            // ToneGenerator lancia RuntimeException se il dispositivo non riesce a inizializzarlo.
            Log.e(TAG, "ToneGenerator non disponibile", e)
            Toast.makeText(this, R.string.audio_unavailable, Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun playBeepSequence(count: Int) {
        if (toneGenerator == null) return
        soundQueue.addLast(count)
        if (isPlayingBeepSequence) return

        isPlayingBeepSequence = true
        remainingBeeps = soundQueue.removeFirst()
        playNextBeep()
    }

    private fun playNextBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
        handler.postDelayed(beepSequenceRunner, BEEP_INTERVAL_MS)
    }

    // ---------- Utilità ----------

    private fun showMessage(messageId: Int) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        const val TAG = "SegnaPuntiLite"
        const val STATE_SCREEN = "screen"
        const val STATE_FINISH_DIALOG_CLOSED = "finish_dialog_closed"
        const val TICK_INTERVAL_MS = 1_000L
        const val TONE_VOLUME = 80
        const val BEEP_DURATION_MS = 350
        const val BEEP_INTERVAL_MS = 500L
    }
}