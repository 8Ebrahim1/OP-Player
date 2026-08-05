package com.opplayer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opplayer.app.data.LocalVideoRepository
import com.opplayer.app.data.VideoFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceVideosUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val folders: List<VideoFolder> = emptyList(),
    val failed: Boolean = false
)

class DeviceVideosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalVideoRepository(application)

    private val _uiState = MutableStateFlow(DeviceVideosUiState())
    val uiState: StateFlow<DeviceVideosUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, failed = false) }

        viewModelScope.launch {
            val result = runCatching { repository.loadFolders() }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    hasLoadedOnce = true,
                    folders = result.getOrDefault(state.folders),
                    failed = result.isFailure
                )
            }
        }
    }
}
