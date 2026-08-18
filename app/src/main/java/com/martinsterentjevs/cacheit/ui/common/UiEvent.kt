package com.martinsterentjevs.cacheit.ui.common

sealed interface UiEvent {
    data class Snackbar(val message: String, val actionLabel: String? = null) : UiEvent
    data class Dialog(
        val title: String,
        val message: String,
        val confirmLabel: String = "OK",
        val onConfirm: () -> Unit = {},
    ) : UiEvent
}