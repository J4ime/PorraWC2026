package com.porrawc2026.app.ui.screens.knockout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KnockoutViewModel @Inject constructor(
    private val repository: PorraRepository
) : ViewModel() {

    val matches: StateFlow<List<MatchEntity>> = repository.getKnockoutMatches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val predictions: StateFlow<List<KnockoutPredictionEntity>> = repository.getKnockoutPredictions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun savePrediction(prediction: KnockoutPredictionEntity) {
        viewModelScope.launch {
            repository.updateKnockoutPrediction(prediction)
        }
    }
}
