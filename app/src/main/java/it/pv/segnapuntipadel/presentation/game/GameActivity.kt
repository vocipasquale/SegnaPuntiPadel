package it.pv.segnapuntipadel.presentation.game

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.AlertDialog
import android.view.KeyEvent
import android.view.WindowManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import it.pv.segnapuntipadel.R
import it.pv.segnapuntipadel.domain.model.GameScore
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.Score
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.remote.RemoteKeyDecision
import it.pv.segnapuntipadel.domain.remote.RemoteKeyMapper
import it.pv.segnapuntipadel.presentation.remote.ExternalInputDevice
import it.pv.segnapuntipadel.presentation.setup.SetupActivity
import kotlinx.coroutines.launch
import java.util.ArrayDeque

class GameActivity : ComponentActivity() {
    private val soundHandler = Handler(Looper.getMainLooper())
    private val soundQueue = ArrayDeque<Int>()
    private var toneGenerator: ToneGenerator? = null
    private var remainingBeeps = 0
    private var isPlayingBeepSequence = false
    private val beepSequenceRunner = object : Runnable {
        override fun run() {
            remainingBeeps--
            when {
                remainingBeeps > 0 -> playNextBeep()
                soundQueue.isNotEmpty() -> {
                    remainingBeeps = soundQueue.removeFirst()
                    playNextBeep()
                }
                else -> isPlayingBeepSequence = false
            }
        }
    }

    private lateinit var viewModel: MatchViewModel
    private lateinit var teamAName: TextView
    private lateinit var teamBName: TextView
    private lateinit var teamAPoints: TextView
    private lateinit var teamBPoints: TextView
    private lateinit var teamAServingIndicator: View
    private lateinit var teamBServingIndicator: View
//    private lateinit var teamAGames: TextView
//    private lateinit var teamBGames: TextView
//    private lateinit var setNumber: TextView
    //private lateinit var setsScore: TextView
    private lateinit var timer: TextView
    private lateinit var setHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = readMatchConfig()
        if (config == null) {
            Toast.makeText(this, R.string.missing_match_data, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        viewModel = ViewModelProvider(this, MatchViewModel.Factory(config))
            .get(MatchViewModel::class.java)
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        setContentView(R.layout.activity_game)
        bindViews()
        bindActions(config)
        observeState(config)
        observeEvents(config)
    }

    private fun bindViews() {
        teamAName = findViewById(R.id.team_a_name)
        teamBName = findViewById(R.id.team_b_name)
        teamAPoints = findViewById(R.id.team_a_points)
        teamBPoints = findViewById(R.id.team_b_points)
        teamAServingIndicator = findViewById(R.id.team_a_serving_indicator)
        teamBServingIndicator = findViewById(R.id.team_b_serving_indicator)
//        teamAGames = findViewById(R.id.team_a_games)
//        teamBGames = findViewById(R.id.team_b_games)
//        setNumber = findViewById(R.id.set_number)
//        setNumber.visibility = View.GONE
//        findViewById<TextView>(R.id.sets_score).visibility = View.GONE
        timer = findViewById(R.id.match_timer)
        setHistory = findViewById(R.id.set_history)
        setHistory.visibility = View.VISIBLE
    }

    @Suppress("DEPRECATION")
    private fun readMatchConfig(): MatchConfig? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_MATCH_CONFIG, MatchConfig::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_MATCH_CONFIG) as? MatchConfig
        }

    private fun bindActions(config: MatchConfig) {
        teamAName.text = config.teamA.displayName()
        teamBName.text = config.teamB.displayName()

        teamAPoints.setOnClickListener { viewModel.scorePoint(TeamSide.A) }
        teamBPoints.setOnClickListener { viewModel.scorePoint(TeamSide.B) }

        teamAPoints.setOnLongClickListener {
            showCorrectionResult(TeamSide.A)
            true
        }
        teamBPoints.setOnLongClickListener {
            showCorrectionResult(TeamSide.B)
            true
        }
    }

    private fun observeState(config: MatchConfig) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state.match, state.elapsedSeconds, config)
                }
            }
        }
    }

    private fun observeEvents(config: MatchConfig) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        MatchEvent.PointScored -> playBeepSequence(1)

                        is MatchEvent.SetCompleted -> {
                            playBeepSequence(2)
                            Toast.makeText(
                                this@GameActivity,
                                getString(
                                    R.string.set_won,
                                    event.setNumber,
                                    config.team(event.winner).displayName()
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is MatchEvent.MatchCompleted -> {
                            playBeepSequence(3)
                            showMatchFinished(
                                event.winner?.let { config.team(it).displayName() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Telecomando Bluetooth: un punto per pressione, ripetizioni ignorate; i tasti volume
    // del telefono non sono intercettati. RestrictedApi è un falso positivo su
    // ComponentActivity.dispatchKeyEvent (metodo pubblico di Activity).
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!::viewModel.isInitialized) return super.dispatchKeyEvent(event)

        val decision = RemoteKeyMapper.decide(
            keyCode = event.keyCode,
            action = event.action,
            repeatCount = event.repeatCount,
            fromExternalDevice = ExternalInputDevice.isExternal(event)
        )
        return when (decision) {
            is RemoteKeyDecision.ScorePoint -> {
                viewModel.scorePoint(decision.side)
                true
            }
            RemoteKeyDecision.Ignore -> true
            RemoteKeyDecision.PassThrough -> super.dispatchKeyEvent(event)
        }
    }

    private fun playBeepSequence(count: Int) {
        soundQueue.addLast(count)
        if (isPlayingBeepSequence) return

        isPlayingBeepSequence = true
        remainingBeeps = soundQueue.removeFirst()
        playNextBeep()
    }

    private fun playNextBeep() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
        soundHandler.postDelayed(beepSequenceRunner, BEEP_INTERVAL_MS)
    }

    override fun onDestroy() {
        soundHandler.removeCallbacks(beepSequenceRunner)
        soundQueue.clear()
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    private fun render(match: MatchState, elapsedSeconds: Long, config: MatchConfig) {
        //setsScore.text = getString(
        //    R.string.match_score,
        //    match.setsWon.a,
        //    match.setsWon.b
        //)
        timer.text = getString(
            R.string.timer_format,
            elapsedSeconds / 60,
            elapsedSeconds % 60
        )

        teamAPoints.text = formatGameScore(match.gameScore, TeamSide.A)
        teamBPoints.text = formatGameScore(match.gameScore, TeamSide.B)
        teamAServingIndicator.visibility =
            if (match.servingSide == TeamSide.A) View.VISIBLE else View.INVISIBLE
        teamBServingIndicator.visibility =
            if (match.servingSide == TeamSide.B) View.VISIBLE else View.INVISIBLE
        teamAPoints.contentDescription = getString(
            R.string.score_label,
            teamAName.text
        )
        teamBPoints.contentDescription = getString(
            R.string.score_label,
            teamBName.text
        )

//        teamAGames.text = getString(R.string.games_for_team, match.games.a)
//        teamBGames.text = getString(R.string.games_for_team, match.games.b)
        setHistory.text = (0 until config.rules.setsToPlay).map { index ->
            when {
                index < match.completedSets.size -> match.completedSets[index].games
                index == match.completedSets.size && !match.isFinished -> match.games
                else -> Score()
            }
        }.joinToString("  ") { "${it.a}-${it.b}" }

        teamAPoints.isEnabled = !match.isFinished
        teamBPoints.isEnabled = !match.isFinished
    }

    private fun formatGameScore(
        gameScore: GameScore,
        side: TeamSide
    ): String {
        if (gameScore.killerPoint) return getString(R.string.killer_point)
        if (gameScore.advantage != null) {
            return if (gameScore.advantage == side) {
                //getString(R.string.advantage_team, getString(teamNameResource(side)))
                "1"
            } else {
                "0"
            }
        }
        if (gameScore.completedDeuceCycles > 0) {
            return getString(R.string.deuce_count, gameScore.completedDeuceCycles)
        }

        return formatPointValue(gameScore.points, side)
    }

    private fun formatPointValue(points: Score, side: TeamSide): String {
        val value = points.forSide(side)
        return when (value) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            else -> "40"
        }
    }

    private fun teamNameResource(side: TeamSide): Int =
        if (side == TeamSide.A) R.string.team_a else R.string.team_b

    private fun showCorrectionResult(side: TeamSide) {
        val teamName = getString(teamNameResource(side))
        val message = if (viewModel.correctLastPoint(side)) {
            getString(R.string.point_corrected, teamName)
        } else {
            getString(R.string.no_point_to_correct, teamName)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showMatchFinished(winnerName: String?) {
        val message = winnerName?.let { getString(R.string.match_won, it) }
            ?: getString(R.string.match_draw)
        AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.new_match) { _, _ ->
                viewModel.startNewMatch()
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finishAffinity()
            }
            .show()
    }

    companion object {
        const val EXTRA_MATCH_CONFIG = "match_config"
        private const val BEEP_DURATION_MS = 350
        private const val BEEP_INTERVAL_MS = 500L
    }
}
