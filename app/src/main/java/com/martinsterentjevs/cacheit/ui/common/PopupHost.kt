package com.martinsterentjevs.cacheit.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow

/** Owns event collection + dialog rendering. Snackbar positioning stays with whatever Scaffold hosts snackbarHostState. */
@Composable
fun PopupHost(events: Flow<UiEvent>, snackbarHostState: SnackbarHostState) {
    var dialogEvent by remember { mutableStateOf<UiEvent.Dialog?>(null) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                )
                is UiEvent.Dialog -> dialogEvent = event
            }
        }
    }

    dialogEvent?.let { dialog ->
        AlertDialog(
            onDismissRequest = { dialogEvent = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = { dialog.onConfirm(); dialogEvent = null }) {
                    Text(dialog.confirmLabel)
                }
            },
        )
    }
}