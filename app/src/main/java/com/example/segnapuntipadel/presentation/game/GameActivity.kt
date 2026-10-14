package com.example.segnapuntipadel.presentation.game

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.app.AlertDialog
import android.view.Gravity
import android.view.WindowManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import com.example.segnapuntipadel.R
import com.example.segnapuntipadel.domain.model.GameScore
import com.example.segnapuntipadel.domain.model.MatchConfig
import com.example.segnapuntipadel.domain.model.MatchState
import com.example.segnapuntipadel.domain.model.Score
import com.example.segnapuntipadel.domain.model.TeamSide
import com.example.segnapuntipadel.presentation.setup.SetupActivity
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {
    private lateinit var viewModel: MatchViewModel
    private lateinit var teamAName: TextView
    private lateinit var teamBName: TextView
    private lateinit var teamAPoints: TextView
    private lateinit var teamBPoints: TextView
//    private lateinit var teamAGames: TextView
//    private lateinit var teamBGames: TextView
    private lateinit var pointTeamA: Button
    private lateinit var pointTeamB: Button
    private lateinit var setNumber: TextView
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
//        teamAGames = findViewById(R.id.team_a_games)
//        teamBGames = findViewById(R.id.team_b_games)
        pointTeamA = findViewById(R.id.point_team_a)
        pointTeamB = findViewById(R.id.point_team_b)
        setNumber = findViewById(R.id.set_number)
        setNumber.visibility = View.GONE
        //setsScore = findViewById(R.id.sets_score)
        findViewById<TextView>(R.id.sets_score).visibility = View.GONE
        timer = findViewById(R.id.match_timer)
        timer.gravity = Gravity.CENTER
        timer.layoutParams = timer.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
        }
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
       // pointTeamA.text = getString(R.string.score_point, getString(R.string.team_a))
        pointTeamA.text = getString(R.string.score_point)
       // pointTeamB.text = getString(R.string.score_point, getString(R.string.team_b))
        pointTeamB.text = getString(R.string.score_point)

        pointTeamA.setOnClickListener { viewModel.scorePoint(TeamSide.A) }
        pointTeamB.setOnClickListener { viewModel.scorePoint(TeamSide.B) }

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
                        is MatchEvent.SetCompleted -> Toast.makeText(
                            this@GameActivity,
                            getString(
                                R.string.set_won,
                                event.setNumber,
                                config.team(event.winner).displayName()
                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        is MatchEvent.MatchCompleted -> showMatchFinished(
                            event.winner?.let { config.team(it).displayName() }
                        )
                    }
                }
            }
        }
    }

    private fun render(match: MatchState, elapsedSeconds: Long, config: MatchConfig) {
        setNumber.text = getString(
            R.string.set_label,
            match.currentSetNumber.coerceAtMost(config.rules.setsToPlay),
            config.rules.setsToPlay
        )
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
        }.joinToString(" ° ") { "${it.a}-${it.b}" }

        pointTeamA.isEnabled = !match.isFinished
        pointTeamB.isEnabled = !match.isFinished
    }

    private fun formatGameScore(
        gameScore: GameScore,
        side: TeamSide
    ): String {
        if (gameScore.killerPoint) return getString(R.string.killer_point)
        if (gameScore.advantage != null) {
            return if (gameScore.advantage == side) {
                getString(R.string.advantage_team, getString(teamNameResource(side)))
            } else {
                "40"
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
    }
}
