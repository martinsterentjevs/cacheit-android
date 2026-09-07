package com.martinsterentjevs.cacheit.ui.auth

import com.martinsterentjevs.cacheit.R
import com.martinsterentjevs.cacheit.data.auth.AuthRepository
import com.martinsterentjevs.cacheit.services.crypto.CryptoService
import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.ui.common.PopupController
import com.martinsterentjevs.cacheit.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cryptoService: CryptoService,
    private val securityService: SecurityService,
    popupController: PopupController,
) : BaseAuthViewModel(popupController) {

    fun submit(name: String, username: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
            popupController.show(UiEvent.Snackbar(R.string.snackbar_error_blank_fields))
            return
        }
        if (password != confirmPassword) {
            popupController.show(UiEvent.Snackbar(R.string.snackbar_error_password_mismatch))
            return
        }

        launchAuthFlow {
            val salt = cryptoService.generateSalt() // also caches it, so the derivations below read it back
            val mek = cryptoService.generateMek()

            val authHash = cryptoService.hashPassword(password)
            val muk = cryptoService.hashMek(password)
            val wrappedMek = cryptoService.wrapMek(mek, muk)

            val deviceId = securityService.getOrCreateDeviceId()
            val deviceName = securityService.getDeviceName()

            val session = authRepository.register(name, username, email, salt, authHash, wrappedMek,deviceId,deviceName)

            securityService.setSecureMek(mek)
            securityService.setSession(session.accountId, session.accessToken, session.refreshToken)
        }
    }
}