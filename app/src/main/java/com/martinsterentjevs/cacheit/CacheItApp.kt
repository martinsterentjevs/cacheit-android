package com.martinsterentjevs.cacheit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.martinsterentjevs.cacheit.ui.common.PopupHost
import com.martinsterentjevs.cacheit.ui.common.PopupHostViewModel
import com.martinsterentjevs.cacheit.ui.navigation.CacheItNavHost
import com.martinsterentjevs.cacheit.ui.navigation.SessionCheckViewModel

@Composable
fun CacheItApp(
    popupHostViewModel: PopupHostViewModel = hiltViewModel(),
    sessionCheckViewModel: SessionCheckViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // contentWindowInsets = WindowInsets(0) — this root Scaffold must not
    // consume any system bar / IME insets itself.
    //
    // Every screen reached via CacheItNavHost already handles its own
    // insets: NoteEditScreen, NoteListScreen, NoteVersionScreen, and
    // AccountOverviewScreen each have their own Scaffold (whose default
    // contentWindowInsets is WindowInsets.safeDrawing), and LoginScreen,
    // RegistrationScreen, and WelcomeScreen apply .safeDrawingPadding()/
    // .imePadding() directly. Before this fix, this root Scaffold was ALSO
    // consuming WindowInsets.safeDrawing by default (Scaffold's own
    // default), so every screen's inset was being subtracted twice —
    // shrinking content, clipping the drawing canvas's touch/render bounds
    // short of the real screen edge, and — because gesture-nav vs.
    // 3-button-nav report different bottom inset heights — visibly
    // shifting the entire app's content whenever the system navigation
    // mode changed.
    //
    // This Scaffold's only jobs now are hosting the snackbar and providing
    // the nav host's container; insets are each screen's own concern.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            CacheItNavHost(startDestination = sessionCheckViewModel.startDestination)
        }
    }

    PopupHost(events = popupHostViewModel.popupController.events, snackbarHostState = snackbarHostState)
}