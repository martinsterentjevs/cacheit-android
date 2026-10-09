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
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow

/** Owns event collection + dialog rendering. Snackbar positioning stays with whatever Scaffold hosts snackbarHostState. */
@Composable
fun PopupHost(
    events: Flow<UiEvent>,
    snackbarHostState: SnackbarHostState
) {
    var pendingSnackbar by remember {
        mutableStateOf<UiEvent.Snackbar?>(null)
    }

    var dialogEvent by remember {
        mutableStateOf<UiEvent.Dialog?>(null)
    }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> pendingSnackbar = event
                is UiEvent.Dialog -> dialogEvent = event
            }
        }
    }

    pendingSnackbar?.let { event ->
        val message = stringResource(
            id = event.messageId,
            *event.formatArgs.toTypedArray()
        )

        LaunchedEffect(event) {
            snackbarHostState.showSnackbar(
                message = message,
                actionLabel = event.actionLabel,
            )

            pendingSnackbar = null
        }
    }

    dialogEvent?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                dialogEvent = null
            },
            title = {
                Text(stringResource(dialog.titleId))
            },
            text = {
                Text(stringResource(dialog.messageId))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialog.onConfirm()
                        dialogEvent = null
                    }
                ) {
                    Text(stringResource(dialog.confirmLabelId))
                }
            }
        )
    }
}