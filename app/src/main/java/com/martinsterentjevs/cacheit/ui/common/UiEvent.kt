package com.martinsterentjevs.cacheit.ui.common

import androidx.annotation.StringRes
import com.martinsterentjevs.cacheit.R

sealed interface UiEvent {

    data class Snackbar(
        @StringRes val messageId: Int,
        val formatArgs: List<Any> = emptyList(),
        val actionLabel: String? = null,
    ) : UiEvent

    data class Dialog(
        @StringRes val titleId: Int,
        @StringRes val messageId: Int,
        @StringRes val confirmLabelId: Int = R.string.ok,
        val onConfirm: () -> Unit = {},
    ) : UiEvent
}