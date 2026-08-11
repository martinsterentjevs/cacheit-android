package com.martinsterentjevs.cacheit.ui.navigation

/**
 * All nav destinations in one place. Route strings are the single source of
 * truth — screens themselves take plain parameters, never raw route strings.
 */
sealed class Route(val route: String) {
    data object Welcome : Route("welcome")
    data object Login : Route("login")
    data object Registration : Route("registration")
    data object NotesList : Route("notes_list")

    data object NoteEdit : Route("note_edit/{noteId}") {
        const val ARG_NOTE_ID = "noteId"
        const val NEW_NOTE_ID = "new"
        fun createRoute(noteId: String = NEW_NOTE_ID) = "note_edit/$noteId"
    }

    data object AccountOverview : Route("account_overview")
}