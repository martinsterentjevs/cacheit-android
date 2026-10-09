package com.martinsterentjevs.cacheit.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.data.auth.AuthFlowException
import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AccountOverviewEvent {
    data object LoggedOut : AccountOverviewEvent
}

@HiltViewModel
class AccountOverviewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val securityService: SecurityService,
    private val wsManager: WsSessionManager,
) : ViewModel() {
    private val _events = Channel<AccountOverviewEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onLogoutTapped() {
        viewModelScope.launch {
            try {
                authRepository.logout() // best-effort server-side revoke
            } catch (e: AuthFlowException) {
                // Swallowed deliberately - local logout must succeed even if this fails
                // (offline, server error, etc). Nothing left for the user to act on here.
            }
            wsManager.disconnect()
            securityService.clearSession()
            securityService.clearSecureMek()
            _events.send(AccountOverviewEvent.LoggedOut)
        }
    }
}