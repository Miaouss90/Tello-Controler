package com.miaouss90.tellocontroler.update

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miaouss90.tellocontroler.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data object NeedsInstallPermission : UpdateState
    data class Downloading(val version: String, val progress: Float?) : UpdateState
    data object Installing : UpdateState
    data object AwaitingConfirmation : UpdateState
    data object Installed : UpdateState
    data class Failed(val message: String) : UpdateState

    val busy: Boolean get() = this is Checking || this is Downloading
}

/** One-tap update: check GitHub → download → install. */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    val installedVersion: String = BuildConfig.VERSION_NAME

    private val source = GitHubReleaseSource()
    private val installer = ApkInstaller(app)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            InstallEvents.latest.collect { event ->
                _state.value = when (event) {
                    null -> return@collect
                    InstallEvents.Event.AwaitingConfirmation -> UpdateState.AwaitingConfirmation
                    InstallEvents.Event.Success -> UpdateState.Installed
                    is InstallEvents.Event.Failed -> UpdateState.Failed(event.message)
                }
            }
        }
    }

    fun installPermissionIntent(): Intent = installer.installPermissionIntent()

    fun update() {
        if (_state.value.busy) return
        InstallEvents.clear()
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UpdateState.Checking
            runCatching {
                val release = source.latest()
                when {
                    release == null || !AppVersion.isNewer(release.tag, installedVersion) ->
                        _state.value = UpdateState.UpToDate(installedVersion)
                    !installer.canInstall() ->
                        _state.value = UpdateState.NeedsInstallPermission
                    else -> {
                        _state.value = UpdateState.Downloading(release.tag, 0f)
                        source.download(release.apkUrl) { apk ->
                            installer.install(apk, release.apkSizeBytes) { p ->
                                _state.value = UpdateState.Downloading(release.tag, p)
                            }
                        }
                        // The receiver may already have reported a result while we were committing.
                        if (_state.value is UpdateState.Downloading) _state.value = UpdateState.Installing
                    }
                }
            }.onFailure {
                _state.value = UpdateState.Failed(it.message ?: "Update failed (no Internet?)")
            }
        }
    }
}
