package com.example.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.app.R
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Space
import kotlinx.coroutines.CancellationException

/** S05, playlists only. Liked songs, saved albums, followed artists, filters and sort come later. */
@Composable
fun LibraryScreen(actions: Actions) {
    var attempt by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    val load = loadAsync("library", attempt = attempt) { actions.container.playlists.list() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.screenMargin, end = Space.s2, top = Space.s2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.nav_library),
                style = AppTheme.type.headline,
                color = AppTheme.colors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = { creating = true }, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.library_create), tint = AppTheme.colors.text)
            }
        }
        when (val l = load) {
            Load.Loading -> LoadingList()
            Load.Failed -> EmptyState(
                Icons.Filled.Warning,
                stringResource(R.string.detail_error_title),
                stringResource(R.string.search_error_hint),
                action = { RetryButton { attempt++ } },
            )
            is Load.Ready -> if (l.value.isEmpty()) {
                EmptyState(
                    Icons.Filled.LibraryMusic,
                    stringResource(R.string.library_empty_title),
                    stringResource(R.string.library_empty_hint),
                    action = { PrimaryButton(stringResource(R.string.library_create), { creating = true }) },
                )
            } else {
                LazyColumn {
                    items(l.value, key = { it.id }) { p -> PlaylistRow(p) { actions.openPlaylist(p.id) } }
                }
            }
        }
    }
    if (creating) {
        CreatePlaylistDialog(
            onSubmit = { name ->
                try {
                    actions.container.playlists.create(name)
                    attempt++
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    false
                }
            },
            onDismiss = { creating = false },
        )
    }
}
