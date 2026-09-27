package com.miaouss90.tellocontroler.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Last PackageInstaller outcome, observed by [UpdateViewModel]. */
object InstallEvents {
    sealed interface Event {
        data object AwaitingConfirmation : Event
        data object Success : Event
        data class Failed(val message: String) : Event
    }

    private val _latest = MutableStateFlow<Event?>(null)
    val latest: StateFlow<Event?> = _latest.asStateFlow()

    fun emit(event: Event) {
        _latest.value = event
    }

    fun clear() {
        _latest.value = null
    }
}

/** Receives the install session result and shows the system confirmation screen when required. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                intent.confirmationIntent()?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                InstallEvents.emit(InstallEvents.Event.AwaitingConfirmation)
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvents.emit(InstallEvents.Event.Success)
            else -> InstallEvents.emit(
                InstallEvents.Event.Failed(
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Installation failed",
                ),
            )
        }
    }

    private fun Intent.confirmationIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_INTENT)
        }
}
