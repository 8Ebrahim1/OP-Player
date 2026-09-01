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
    val failed: Boolean = false,
    val openFolderId: Long? = null,
    val query: String = ""
)

class DeviceVideosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalVideoRepository(application)

    private val _uiState = MutableStateFlow(DeviceVideosUiState())
    val uiState: StateFlow<DeviceVideosUiState> = _uiState.asStateFlow()

    // Scroll offsets are plain fields rather than part of the ui state: the player replaces the
    // whole screen, so remembered list state is dropped, while folding them into the state flow
    // would recompose the list on every scrolled pixel.
    var folderScrollIndex: Int = 0
        private set

    var folderScrollOffset: Int = 0
        private set

    var videoScrollIndex: Int = 0
        private set

    var videoScrollOffset: Int = 0
        private set

    private var videoScrollFolderId: Long? = null

    fun rememberFolderScroll(index: Int, offset: Int) {
        folderScrollIndex = index
        folderScrollOffset = offset
    }

    fun rememberVideoScroll(index: Int, offset: Int) {
        videoScrollIndex = index
        videoScrollOffset = offset
    }

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

    // The open folder and its search text live here instead of in the composable, because the
    // player replaces the whole screen: remembered state would be dropped on the way in and the
    // user would land back on the folder list after closing the video.
    fun openFolder(folderId: Long) {
        if (videoScrollFolderId != folderId) {
            videoScrollFolderId = folderId
            videoScrollIndex = 0
            videoScrollOffset = 0
        }

        _uiState.update { it.copy(openFolderId = folderId, query = "") }
    }

    fun closeFolder() {
        _uiState.update { it.copy(openFolderId = null, query = "") }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }
}
