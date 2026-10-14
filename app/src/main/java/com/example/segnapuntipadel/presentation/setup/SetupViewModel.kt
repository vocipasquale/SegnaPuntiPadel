package com.example.segnapuntipadel.presentation.setup

import androidx.lifecycle.ViewModel
import com.example.segnapuntipadel.domain.model.MatchConfig
import com.example.segnapuntipadel.domain.model.MatchRules
import com.example.segnapuntipadel.domain.model.Team

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
