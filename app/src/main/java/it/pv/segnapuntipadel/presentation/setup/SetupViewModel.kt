package it.pv.segnapuntipadel.presentation.setup

import androidx.lifecycle.ViewModel
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchRules
import it.pv.segnapuntipadel.domain.model.Team

class SetupViewModel : ViewModel() {
    private var teamA = Team("", "")
    private var teamB = Team("", "")

    fun saveTeams(teamA: Team, teamB: Team) {
        this.teamA = teamA
        this.teamB = teamB
    }

    fun createMatchConfig(rules: MatchRules): MatchConfig =
        MatchConfig(teamA = teamA, teamB = teamB, rules = rules)
}
