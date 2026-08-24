package com.martinsterentjevs.cacheit

import androidx.compose.foundation.layout.Box
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

@Composable
fun CacheItApp(popupHostViewModel: PopupHostViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            CacheItNavHost()
        }
    }

    PopupHost(events = popupHostViewModel.popupController.events, snackbarHostState = snackbarHostState)
}