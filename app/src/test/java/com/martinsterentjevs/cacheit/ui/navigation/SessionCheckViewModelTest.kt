package com.martinsterentjevs.cacheit.ui.navigation

import com.martinsterentjevs.cacheit.services.security.SecurityService
import com.martinsterentjevs.cacheit.services.websockets.WsSessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCheckViewModelTest {

    @Test
    fun `routes to NoteList and connects the websocket when a persisted access token exists`() {
        val securityService = mockk<SecurityService> { every { getAccessToken() } returns "a-token" }
        val wsManager = mockk<WsSessionManager>(relaxed = true)

        val viewModel = SessionCheckViewModel(securityService, wsManager)

        assertEquals(Route.NoteList.route, viewModel.startDestination)
        verify { wsManager.connectIfNeeded() }
    }

    @Test
    fun `routes to Welcome and does not touch the websocket when there is no persisted token`() {
        val securityService = mockk<SecurityService> { every { getAccessToken() } returns null }
        val wsManager = mockk<WsSessionManager>(relaxed = true)

        val viewModel = SessionCheckViewModel(securityService, wsManager)

        assertEquals(Route.Welcome.route, viewModel.startDestination)
        verify(exactly = 0) { wsManager.connectIfNeeded() }
    }
}