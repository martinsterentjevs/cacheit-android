package com.martinsterentjevs.cacheit.ui.auth

import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cryptoService: CryptoService,
    private val securityService: SecurityService,
    popupController: PopupController,
) : BaseAuthViewModel(popupController) {

    fun submit(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            popupController.show(UiEvent.Snackbar("Enter your email/username and password"))
            return
        }
        launchAuthFlow {
            // Two-round-trip login: fetchSalt caches the real (or keyed-fake) salt,
            // then both derivations below read it back via CryptoService internally.
            val salt = authRepository.fetchSalt(identifier)
            securityService.setSalt(salt)

            val authHash = cryptoService.hashPassword(password)
            val muk = cryptoService.hashMek(password)
            val deviceId = securityService.getOrCreateDeviceId()
            val deviceName = securityService.getDeviceName()
            val session = authRepository.login(identifier, authHash,deviceId,deviceName)

            val mek = cryptoService.unwrapMek(session.wrappedMek, muk)
            securityService.setSecureMek(mek)
            securityService.setSession(session.accountId, session.accessToken, session.refreshToken)
        }
    }
}