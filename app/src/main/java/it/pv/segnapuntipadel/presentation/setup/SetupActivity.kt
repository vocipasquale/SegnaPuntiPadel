package it.pv.segnapuntipadel.presentation.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import it.pv.segnapuntipadel.R
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.Team
import it.pv.segnapuntipadel.presentation.game.GameActivity
import it.pv.segnapuntipadel.presentation.remote.RemoteTestActivity

class SetupActivity : ComponentActivity() {
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_setup)

        val teamPanel = findViewById<View>(R.id.team_setup_panel)
        val rulesPanel = findViewById<View>(R.id.rules_setup_panel)
        val playerA1 = findViewById<EditText>(R.id.player_a1)
        val playerA2 = findViewById<EditText>(R.id.player_a2)
        val playerB1 = findViewById<EditText>(R.id.player_b1)
        val playerB2 = findViewById<EditText>(R.id.player_b2)
        val setsToPlay = findViewById<EditText>(R.id.sets_to_play)
        val maxGamesPerSet = findViewById<EditText>(R.id.max_games_per_set)
        val maxDeuceCycles = findViewById<EditText>(R.id.max_deuce_cycles)

        findViewById<Button>(R.id.continue_to_rules).setOnClickListener {
            viewModel.saveTeams(
                teamA = Team(playerA1.text.toString().trim(), playerA2.text.toString().trim()),
                teamB = Team(playerB1.text.toString().trim(), playerB2.text.toString().trim())
            )
            teamPanel.visibility = View.GONE
            rulesPanel.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.open_remote_test).setOnClickListener {
            startActivity(Intent(this, RemoteTestActivity::class.java))
        }

        findViewById<Button>(R.id.back_to_teams).setOnClickListener {
            rulesPanel.visibility = View.GONE
            teamPanel.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.start_match).setOnClickListener {
            val sets = setsToPlay.text.toString().toIntOrNull()
            val maxGames = maxGamesPerSet.text.toString().toIntOrNull()
            val maxDeuces = maxDeuceCycles.text.toString().toIntOrNull()

            if (sets == null || maxGames == null || maxDeuces == null) {
                showMessage(R.string.invalid_number)
                return@setOnClickListener
            }
            if (sets < 1 || maxDeuces < 1) {
                showMessage(R.string.invalid_positive_values)
                return@setOnClickListener
            }
            if (maxGames < 6) {
                showMessage(R.string.invalid_max_games)
                return@setOnClickListener
            }

            val config = viewModel.createMatchConfig(
                MatchRules(
                    setsToPlay = sets,
                    maxGamesPerSet = maxGames,
                    maxDeuceCycles = maxDeuces
                )
            )
            startActivity(
                Intent(this, GameActivity::class.java)
                    .putExtra(GameActivity.EXTRA_MATCH_CONFIG, config)
            )
            finish()
        }
    }

    private fun showMessage(messageId: Int) {
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show()
    }
}
