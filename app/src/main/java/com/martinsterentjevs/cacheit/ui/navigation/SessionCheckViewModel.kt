// ui/navigation/SessionCheckViewModel.kt
package com.martinsterentjevs.cacheit.ui.navigation

import androidx.lifecycle.ViewModel
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionCheckViewModel @Inject constructor(
    securityService: SecurityService,
    wsManager: WsSessionManager
) : ViewModel() {
    // Presence, not validity - an expired-but-present access token still routes to
    // NotesList; TokenAuthenticator's refresh-on-401 (now wired in) handles the rest
    // transparently on the first real request.
    val startDestination: String = if (securityService.getAccessToken() != null) {
        //Initiate WebSockets on app boot when logged in here
        wsManager.connectIfNeeded()
        Route.NoteList.route
    } else {
        Route.Welcome.route
    }
}