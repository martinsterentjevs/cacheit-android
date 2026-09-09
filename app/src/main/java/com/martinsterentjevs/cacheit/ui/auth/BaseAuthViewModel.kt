package com.martinsterentjevs.cacheit.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.auth.AuthFlowException
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
}

sealed interface AuthEvent {
    data object Success : AuthEvent
}


/**
 * Shared submit-flow scaffolding for Login and Registration.
 * See docs/decisions/0001-ViewModel-Setup-setup.md for why this is a base
 * class rather than duplicated or composed in.
 */
abstract class BaseAuthViewModel(
    protected val popupController: PopupController,
    protected val wsManager: WsSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    // Channel, not SharedFlow - a nav event must fire exactly once per submit,
    // never replayed on recomposition/rotation the way a StateFlow would be.
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    protected fun launchAuthFlow(block: suspend () -> Unit) {
        if (_uiState.value is AuthUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                block()
                _events.send(AuthEvent.Success)
                //Trigger WebSocket Launch on auth success
                wsManager.connectIfNeeded()
            } catch (e: AuthFlowException) {
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error,listOf(e.userMessage)))
            } catch (e: Exception) {
                android.util.Log.e("AuthFlow","Unhandled auth error",e)
                popupController.show(UiEvent.Snackbar(R.string.snackbar_error_general))
            } finally {
                _uiState.value = AuthUiState.Idle
            }
        }
    }

}