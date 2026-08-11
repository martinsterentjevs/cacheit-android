package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItSpacing
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.TypeBody

/**
 * Note edit screen — handles create, view, and edit in a single screen. There is
 * no per-note unlock gate; the vault-level session already covers that.
 *
 * [noteId] is "new" for a freshly created note, or an existing note's id to load.
 *
 * Contents (not yet implemented):
 * - Title + body fields, encrypted client-side before sync
 * - Drawing entry point (separate MVP item, not part of this screen's initial scope)
 * - Version history bottom sheet (not a separate nav route)
 * - Autosave / dirty-state handling
 */
@OptIn(ExperimentalMaterial3Api::class) //Temporary workaround
@Composable
fun NoteEditScreen(
    noteId: String = "new",
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == "new") "New note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(innerPadding)
                .padding(CacheItSpacing.lg),
        ) {
            // TODO: title field, body field, encryption wiring
            Text("Note content placeholder (noteId=$noteId)", style = TypeBody)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoteEditScreenPreview() {
    CacheItTheme {
        NoteEditScreen()
    }
}