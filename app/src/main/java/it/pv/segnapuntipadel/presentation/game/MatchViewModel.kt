package it.pv.segnapuntipadel.presentation.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.pv.segnapuntipadel.domain.model.MatchAction
import it.pv.segnapuntipadel.domain.model.MatchConfig
import it.pv.segnapuntipadel.domain.model.MatchState
import it.pv.segnapuntipadel.domain.model.TeamSide
import it.pv.segnapuntipadel.domain.usecase.ApplyMatchAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameUiState(
    val match: MatchState,
    val elapsedSeconds: Long
)

sealed class MatchEvent {
    object PointScored : MatchEvent()
    data class SetCompleted(val setNumber: Int, val winner: TeamSide) : MatchEvent()
    data class MatchCompleted(val winner: TeamSide?) : MatchEvent()
}

class MatchViewModel(
    private val config: MatchConfig,
    private val applyMatchAction: ApplyMatchAction = ApplyMatchAction()
) : ViewModel() {
    private val initialState = MatchState(config)
    private var startedAtElapsedRealtime = SystemClock.elapsedRealtime()
    private data class RecordedAction(
        val action: MatchAction,
        val scoresPoint: Boolean
    )

    private val actions = mutableListOf<RecordedAction>()
    private val _uiState = MutableStateFlow(GameUiState(initialState, 0))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MatchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MatchEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val elapsed = (SystemClock.elapsedRealtime() - startedAtElapsedRealtime) / 1_000
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
    }

    fun scorePoint(side: TeamSide) {
        val action = MatchAction.ScorePoint(side)
        val previous = _uiState.value.match
        val updated = applyMatchAction.execute(previous, action)
        if (updated == previous) return

        actions += RecordedAction(action, previous.servingSide != null)
        _uiState.update { it.copy(match = updated) }

        if (updated.completedSets.size > previous.completedSets.size) {
            val completed = updated.completedSets.last()
            if (updated.isFinished) {
                _events.tryEmit(MatchEvent.MatchCompleted(updated.winner))
            } else {
                _events.tryEmit(
                    MatchEvent.SetCompleted(updated.completedSets.size, completed.winner)
                )
            }
        } else {
            _events.tryEmit(MatchEvent.PointScored)
        }
    }

    fun correctLastPoint(side: TeamSide): Boolean {
        val actionIndex = actions.indexOfLast {
            val action = it.action
            it.scoresPoint && action is MatchAction.ScorePoint && action.side == side
        }
        if (actionIndex < 0) return false

        actions.removeAt(actionIndex)
        val corrected = applyMatchAction.replay(initialState, actions.map { it.action })
        _uiState.update { it.copy(match = corrected) }
        return true
    }

    fun startNewMatch() {
        actions.clear()
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        _uiState.value = GameUiState(MatchState(config), 0)
    }

    class Factory(private val config: MatchConfig) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MatchViewModel::class.java)) {
                return MatchViewModel(config) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
