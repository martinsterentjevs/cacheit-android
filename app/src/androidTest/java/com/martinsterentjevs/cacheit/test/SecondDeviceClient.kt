package com.martinsterentjevs.cacheit.test

import androidx.test.platform.app.InstrumentationRegistry
import com.martinsterentjevs.cacheit.BuildConfig
import com.martinsterentjevs.cacheit.network.auth.AccountSessionResponse
import com.martinsterentjevs.cacheit.network.auth.AuthApi
import com.martinsterentjevs.cacheit.network.auth.LoginRequest
import com.martinsterentjevs.cacheit.network.auth.SaltLookupRequest
import com.martinsterentjevs.cacheit.network.cacheItJson
import com.martinsterentjevs.cacheit.network.note.NoteApi
import com.martinsterentjevs.cacheit.services.crypto.ArgonWrapper
import com.martinsterentjevs.cacheit.services.security.CacheAlias
import com.martinsterentjevs.cacheit.services.security.KeyStoreService
import com.martinsterentjevs.cacheit.services.security.LocalCacheKey
import com.martinsterentjevs.cacheit.services.security.LocalStore
import com.martinsterentjevs.cacheit.services.security.SecurityService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.Base64
import java.util.UUID

/**
 * Simulates a second device logging into the SAME account as the real, UI-driven "device 1",
 * for Paths D/E (two-device sync). Deliberately does NOT reuse the app's real Hilt-injected
 * AuthApi/NoteApi/SecurityService - those are process-wide singletons bound to exactly one
 * session:
 *
 *  - AuthInterceptor reads the CURRENT access token from the shared SecurityService on every
 *    request, so calls made through the injected NoteApi would silently use device 1's token.
 *  - SecurityService's local cache is backed by a hardcoded SharedPreferences file name
 *    ("cacheit_local_store") and a hardcoded Keystore alias - even constructing a SECOND
 *    SecurityService instance pointed at real storage would read/write the exact same
 *    underlying data as device 1's, corrupting both sessions.
 *
 * Instead this builds its own Retrofit/OkHttp stack (own in-memory token holder) and reuses
 * the real ArgonWrapper class UNCHANGED - so the auth-hash derivation can never drift from
 * production - against an isolated, in-memory-backed SecurityService whose salt never touches
 * real device storage.
 *
 * Deliberately does NOT unwrap the MEK or decrypt note content: per the checklist, Paths D/E
 * only need noteId presence and HTTP status codes (sync visibility, lock-conflict 409), not
 * plaintext. If a future path needs decrypted content from device 2, AESWrapper's unwrapMek
 * is a pure function (no SecurityService dependency) and can be added without needing the MEK
 * session cache at all.
 */
class SecondDeviceClient {

    val deviceId: String = UUID.randomUUID().toString()
    private val deviceName = "Second Device (test)"

    private var accessToken: String? = null

    private val tokenInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = accessToken
        val authorized = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        chain.proceed(authorized)
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SERVER_BASE_URL)
        .client(OkHttpClient.Builder().addInterceptor(tokenInterceptor).build())
        .addConverterFactory(cacheItJson.asConverterFactory("application/json".toMediaType()))
        .build()

    private val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    /** Raw NoteApi for this device - deliberately public so tests can assert on noteId presence and HTTP status codes directly. */
    val noteApi: NoteApi = retrofit.create(NoteApi::class.java)

    // Isolated salt cache for ArgonWrapper - never touches the real app's SharedPreferences.
    // The KeyStoreService is a throwing stub since this client never calls getSecureMek/
    // setSecureMek (see class doc - MEK unwrap isn't needed for Paths D/E as specced).
    private val isolatedSecurityService = SecurityService(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        keyStoreService = UnusedKeyStoreService,
        localStore = InMemoryLocalStore(),
    )
    private val argonWrapper = ArgonWrapper(isolatedSecurityService)

    /**
     * Logs into an already-registered account as this second, independent device. Performs the
     * real salt-fetch -> Argon2id-derive -> login flow, matching what LoginViewModel does -
     * just without touching the app's real session state.
     */
    suspend fun login(identifier: String, password: String) {
        val saltResponse = authApi.fetchSalt(SaltLookupRequest(identifier))
        isolatedSecurityService.setSalt(Base64.getDecoder().decode(saltResponse.kdfSalt))

        val authHash = argonWrapper.hashPassword(password)
        val session: AccountSessionResponse = authApi.login(
            LoginRequest(
                identifier = identifier,
                authHash = Base64.getEncoder().encodeToString(authHash),
                deviceId = deviceId,
                deviceName = deviceName,
            ),
        )
        accessToken = session.accessToken
    }
}

private class InMemoryLocalStore : LocalStore {
    private val values = mutableMapOf<LocalCacheKey, Any>()

    override fun putBytes(key: LocalCacheKey, value: ByteArray) {
        values[key] = value
    }

    override fun getBytes(key: LocalCacheKey): ByteArray? = values[key] as? ByteArray

    override fun putString(key: LocalCacheKey, value: String) {
        values[key] = value
    }

    override fun getString(key: LocalCacheKey): String? = values[key] as? String

    override fun remove(key: LocalCacheKey) {
        values.remove(key)
    }
}

/** SecondDeviceClient never calls getSecureMek/setSecureMek - any call here is a bug, so it throws loudly instead of silently touching real Keystore aliases. */
private object UnusedKeyStoreService : KeyStoreService {
    override fun wrap(alias: CacheAlias, plaintext: ByteArray): ByteArray =
        error("SecondDeviceClient does not support Keystore-backed values - see class doc")

    override fun unwrap(alias: CacheAlias, envelope: ByteArray): ByteArray =
        error("SecondDeviceClient does not support Keystore-backed values - see class doc")

    override fun invalidate(alias: CacheAlias) = Unit
}