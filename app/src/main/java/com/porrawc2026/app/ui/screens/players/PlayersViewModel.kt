package com.porrawc2026.app.ui.screens.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val repository: PorraRepository
) : ViewModel() {

    val predictions: StateFlow<List<PlayerPredictionEntity>> = repository.getPlayerPredictions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun savePlayer(prediction: PlayerPredictionEntity) {
        viewModelScope.launch {
            repository.updatePlayerPrediction(prediction)
        }
    }
}
