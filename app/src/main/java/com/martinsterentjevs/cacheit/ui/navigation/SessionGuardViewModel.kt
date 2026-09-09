package com.martinsterentjevs.cacheit.ui.navigation

import androidx.lifecycle.ViewModel
import com.martinsterentjevs.cacheit.services.security.SessionExpiredSignal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionGuardViewModel @Inject constructor(
    sessionExpiredSignal: SessionExpiredSignal
) : ViewModel() {
    val expired = sessionExpiredSignal.expired
}