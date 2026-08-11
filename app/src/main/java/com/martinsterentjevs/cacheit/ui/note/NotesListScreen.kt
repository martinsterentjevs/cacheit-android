package com.martinsterentjevs.cacheit.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.martinsterentjevs.cacheit.ui.theme.CacheItTheme
import com.martinsterentjevs.cacheit.ui.theme.TypeBody

/**
 * Notes list — the post-login home screen.
 *
 * Contents (not yet implemented):
 * - Note cards, synced via delta sync
 * - Empty state copy below is final; the CTA button that should pair with it is not
 * - Inline sync-status indicator (there is no separate sync screen by design)
 */
@OptIn(ExperimentalMaterial3Api::class)// Temporary workaround
@Composable
fun NotesListScreen(
    onOpenNote: (noteId: String) -> Unit = {},
    onCreateNote: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                actions = {
                    IconButton(onClick = onOpenAccount) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: replace with the real note list/grid once note CRUD client is wired up
            Text("No notes added yet.", style = TypeBody)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview() {
    CacheItTheme {
        NotesListScreen()
    }
}