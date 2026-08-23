package com.martinsterentjevs.cacheit.ui.navigation

import androidx.compose.runtime.Composable
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
import com.martinsterentjevs.cacheit.ui.note.NoteVersionScreen
import com.martinsterentjevs.cacheit.ui.note.NotesListScreen
import com.martinsterentjevs.cacheit.ui.onboarding.WelcomeScreen

/**
 * Single NavHost for the app. MainActivity hosts this and nothing else — every
 * screen is a plain composable, never its own Activity.
 *
 * Auth-gated start destination (skip Welcome if a session already exists) is a
 * TODO once session-restore logic exists on the crypto/auth side.
 */
@Composable
fun CacheItNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Route.Welcome.route,
) {
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
                    navController.navigate(Route.NotesList.route) {
                        popUpTo(Route.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Route.Registration.route) },
            )
        }

        composable(Route.Registration.route) {
            RegistrationScreen(
                onRegistrationSuccess = {
                    navController.navigate(Route.NotesList.route) {
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
                    navController.popBackStack(Route.NotesList.route, inclusive = false)
                },
            )
        }

        composable(route = Route.NotesList.route) {
            NotesListScreen(
                onCreateNote = { navController.navigate(Route.NoteEdit.createRoute()) },
                onEditNote = { noteId -> navController.navigate(Route.NoteEdit.createRoute(noteId)) },
                onOpenAccount = { navController.navigate(Route.AccountOverview.route) },
                onHistory = { noteId -> navController.navigate(Route.NoteHistory.createRoute(noteId))}
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