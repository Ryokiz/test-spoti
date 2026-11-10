package com.example.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.R
import com.example.app.domain.Track
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Stub Home: "Recently added" only. Shelves, recents and suggestions come in M2.
 * The Sign out button is temporary: it moves to Settings (S18) when that screen exists.
 */
@Composable
fun HomeScreen(actions: Actions) {
    val container = actions.container
    val player by container.player.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<Track>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        failed = false
        tracks = null
        try {
            tracks = container.catalog.allTracks()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.screenMargin, end = Space.s2, top = Space.s2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.home_recent),
                style = AppTheme.type.headline,
                color = AppTheme.colors.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = {
                container.player.stop()
                scope.launch { container.auth.signOut() }
            }) {
                Text(stringResource(R.string.home_sign_out), style = AppTheme.type.label, color = AppTheme.colors.accent)
            }
        }
        val list = tracks
        when {
            failed -> EmptyState(
                Icons.Filled.Warning,
                stringResource(R.string.home_error_title),
                stringResource(R.string.search_error_hint),
                action = { RetryButton { attempt++ } },
            )
            list == null -> LoadingList()
            else -> LazyColumn {
                itemsIndexed(list, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.currentId == track.id,
                        onClick = { container.player.play(list, index) },
                        actions = actions,
                    )
                }
            }
        }
    }
}
