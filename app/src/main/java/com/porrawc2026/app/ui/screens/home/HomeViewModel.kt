package com.porrawc2026.app.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.util.ExcelData
import com.porrawc2026.app.util.ExcelParser
import com.porrawc2026.app.util.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PorraRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _groupPoints = MutableStateFlow(0)
    val groupPoints: StateFlow<Int> = _groupPoints.asStateFlow()

    private val _knockoutPoints = MutableStateFlow(0)
    val knockoutPoints: StateFlow<Int> = _knockoutPoints.asStateFlow()

    private val _questionPoints = MutableStateFlow(0)
    val questionPoints: StateFlow<Int> = _questionPoints.asStateFlow()

    private val _playerPoints = MutableStateFlow(0)
    val playerPoints: StateFlow<Int> = _playerPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    private val _hasData = MutableStateFlow(false)
    val hasData: StateFlow<Boolean> = _hasData.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        refreshPoints()
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _validationResult.value = null

            try {
                val data = ExcelParser.parse(context, uri)
                val validation = ExcelParser.validate(data)
                _validationResult.value = validation

                if (validation.isValid) {
                    repository.insertAllData(
                        data.teams,
                        data.matches,
                        data.questions,
                        data.playerPredictions,
                        data.knockoutPredictions,
                        data.standings
                    )
                    _hasData.value = true
                    refreshPoints()
                } else {
                    repository.insertAllData(
                        data.teams,
                        data.matches,
                        data.questions,
                        data.playerPredictions,
                        data.knockoutPredictions,
                        data.standings
                    )
                    _hasData.value = true
                    refreshPoints()
                }
            } catch (e: Exception) {
                _errorMessage.emit("Error al cargar el Excel: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissValidation() {
        _validationResult.value = null
    }

    fun refreshPoints() {
        viewModelScope.launch {
            _totalPoints.value = repository.calculateTotalPoints()
        }
    }

    fun refreshLiveScores() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshPoints()
            } catch (e: Exception) {
                _errorMessage.emit("Error al actualizar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
