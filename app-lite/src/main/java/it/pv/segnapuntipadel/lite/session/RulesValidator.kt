package it.pv.segnapuntipadel.lite.session

import it.pv.segnapuntipadel.domain.model.MatchRules

sealed class RulesValidation {
    data class Valid(val rules: MatchRules) : RulesValidation()
    object InvalidNumber : RulesValidation()
    object NonPositive : RulesValidation()
    object MaxGamesTooLow : RulesValidation()
}

/** Stesse regole di validazione della schermata di setup dell'app originale. */
object RulesValidator {
    const val MIN_MAX_GAMES_PER_SET = 6

    fun parse(text: String): Int? = text.trim().toIntOrNull()

    fun validate(sets: Int?, maxGames: Int?, maxDeuces: Int?): RulesValidation {
        if (sets == null || maxGames == null || maxDeuces == null) {
            return RulesValidation.InvalidNumber
        }
        if (sets < 1 || maxDeuces < 1) return RulesValidation.NonPositive
        if (maxGames < MIN_MAX_GAMES_PER_SET) return RulesValidation.MaxGamesTooLow
        return RulesValidation.Valid(
            MatchRules(
                setsToPlay = sets,
                maxGamesPerSet = maxGames,
                maxDeuceCycles = maxDeuces
            )
        )
    }

    fun validate(rules: MatchRules): RulesValidation =
        validate(rules.setsToPlay, rules.maxGamesPerSet, rules.maxDeuceCycles)
}