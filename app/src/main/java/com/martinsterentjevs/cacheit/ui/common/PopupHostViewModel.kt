package com.martinsterentjevs.cacheit.ui.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PopupHostViewModel @Inject constructor(
    val popupController: PopupController,
) : ViewModel()