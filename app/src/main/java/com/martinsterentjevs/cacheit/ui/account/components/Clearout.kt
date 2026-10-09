package com.martinsterentjevs.cacheit.ui.account.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.note.NoteRepository
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val HOLD_DURATION_MS = 10_000L

sealed interface ClearOutEvent {
    /** Local wipe done - caller navigates to Welcome, same as logout. */
    data object LocalWipeComplete : ClearOutEvent
}

@HiltViewModel
class ClearOutViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val securityService: SecurityService,
    private val popupController: PopupController,
    private val wsManager: WsSessionManager,
) : ViewModel() {
    private val _events = Channel<ClearOutEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Tap - immediate, in-app only. No server call, no confirmation dialog per the
     *  design decision that the gesture itself (a deliberate tap on this specific control,
     *  distinct from the 10s hold) is the confirmation. */
    fun onTapWipe() {
        viewModelScope.launch {
            noteRepository.clearLocalCache()
            wsManager.disconnect()
            securityService.clearSession()
            securityService.clearSecureMek()
            _events.send(ClearOutEvent.LocalWipeComplete)
        }
    }

    /** Hold-10s - server-side account deletion. Blocked: no server endpoint exists yet
     *  (cleanup-issue scope, per pre-mvp-checklist). Deliberately does nothing destructive
     *  locally when this fires - a completed hold gesture with no backing implementation
     *  should not silently wipe local data as a consolation action. */
    fun onHoldCompleted() {
        popupController.show(UiEvent.Snackbar(R.string.clearout_deletion_unavailable))
    }
}

@Composable
fun ClearOut(viewModel: ClearOutViewModel = hiltViewModel()) {
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed && holdProgress < 1f) {
                holdProgress = ((System.currentTimeMillis() - startTime) / HOLD_DURATION_MS.toFloat())
                    .coerceIn(0f, 1f)
                delay(16.milliseconds)
            }
            if (holdProgress >= 1f) viewModel.onHoldCompleted()
        } else {
            holdProgress = 0f
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.clearout_deletion_notice),
            modifier = Modifier.padding(bottom = CacheItSpacing.sm),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { viewModel.onTapWipe() },
                    )
                }
                .padding(CacheItSpacing.md),
        ) {
            Text(stringResource(R.string.clearout_title), color = MaterialTheme.colorScheme.error)
            if (holdProgress > 0f) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}