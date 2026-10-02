package com.martinsterentjevs.cacheit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.martinsterentjevs.cacheit.ui.account.AccountOverviewScreen
import com.martinsterentjevs.cacheit.ui.auth.LoginScreen
import com.martinsterentjevs.cacheit.ui.auth.RegistrationScreen
import com.martinsterentjevs.cacheit.ui.note.NoteEditScreen
import com.martinsterentjevs.cacheit.ui.note.NoteListScreen
import com.martinsterentjevs.cacheit.ui.note.NoteVersionScreen
import com.martinsterentjevs.cacheit.ui.onboarding.WelcomeScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CacheItNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Route.Welcome.route,
    sessionGuardViewModel: SessionGuardViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        sessionGuardViewModel.expired.collectLatest {
            navController.navigate(Route.Welcome.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Welcome.route) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.Registration.route) },
                onLogin = { navController.navigate(Route.Login.route) },
            )
        }

        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.NoteList.route) {
                        popUpTo(Route.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Route.Registration.route) },
            )
        }

        composable(Route.Registration.route) {
            RegistrationScreen(
                onRegistrationSuccess = {
                    navController.navigate(Route.NoteList.route) {
                        popUpTo(Route.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Route.Login.route) },
            )
        }

        composable(
            route = Route.NoteHistory.route,
            arguments = listOf(
                navArgument(Route.NoteHistory.ARG_NOTE_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString(Route.NoteHistory.ARG_NOTE_ID)
                ?: return@composable

            NoteVersionScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onRestored = {
                    navController.popBackStack(Route.NoteList.route, inclusive = false)
                },
            )
        }
        composable(route = Route.NoteList.route) {
            NoteListScreen(
                onEditNote = { noteId -> navController.navigate(Route.NoteEdit.createRoute(noteId)) },
                onCreateNote = { navController.navigate(Route.NoteEdit.createRoute()) },
                onOpenAccount = { navController.navigate(Route.AccountOverview.route) },
                onHistory = { noteId -> navController.navigate(Route.NoteHistory.createRoute(noteId)) }
            )
        }

        composable(
            route = Route.NoteEdit.route,
            arguments = listOf(navArgument(Route.NoteEdit.ARG_NOTE_ID) {
                type = NavType.StringType
            }),
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString(Route.NoteEdit.ARG_NOTE_ID)
                ?: Route.NoteEdit.NEW_NOTE_ID
            NoteEditScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.AccountOverview.route) {
            AccountOverviewScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Route.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}