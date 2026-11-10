package com.example.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.StringRes
import com.example.app.R
import com.example.app.domain.Playlist
import com.example.app.domain.Track
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import com.example.app.ui.theme.Space
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAX_PLAYLIST_NAME = 100

/** S15: the song menu. Opens S12 (add to playlist) in place when that action is chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuSheet(track: Track, actions: Actions, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val queued = stringResource(R.string.msg_added_queue)
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surfaceRaised, contentColor = c.text) {
        if (picking) {
            AddToPlaylistContent(track, actions, onDone = onDismiss)
        } else {
            Column(Modifier.padding(horizontal = Dimens.screenMargin, vertical = Space.s2)) {
                Text(track.title, style = AppTheme.type.title, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = AppTheme.type.bodySm, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MenuAction(Icons.AutoMirrored.Filled.QueueMusic, R.string.menu_add_queue) {
                actions.container.player.addToQueue(track)
                actions.message(queued)
                onDismiss()
            }
            MenuAction(Icons.AutoMirrored.Filled.PlaylistAdd, R.string.menu_add_playlist) { picking = true }
            if (track.albumId.isNotBlank()) {
                MenuAction(Icons.Filled.Album, R.string.menu_go_album) {
                    onDismiss()
                    actions.openAlbum(track.albumId)
                }
            }
            if (track.artistId.isNotBlank()) {
                MenuAction(Icons.Filled.Person, R.string.menu_go_artist) {
                    onDismiss()
                    actions.openArtist(track.artistId)
                }
            }
        }
        Spacer(Modifier.height(Space.s4))
    }
}

@Composable
private fun MenuAction(icon: ImageVector, @StringRes label: Int, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.touchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.screenMargin, vertical = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text)
        Spacer(Modifier.width(Space.s4))
        Text(stringResource(label), style = AppTheme.type.body, color = c.text)
    }
}

/** S12: pick a playlist for [track], or make a new one. */
@Composable
private fun AddToPlaylistContent(track: Track, actions: Actions, onDone: () -> Unit) {
    val c = AppTheme.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var attempt by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    val load = loadAsync("sheet-playlists", attempt = attempt) { actions.container.playlists.list() }

    fun addTo(p: Playlist) {
        scope.launch {
            val msg = try {
                val added = actions.container.playlists.addTracks(p.id, listOf(track))
                ctx.getString(if (added > 0) R.string.msg_added_to_playlist else R.string.msg_already_in_playlist, p.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ctx.getString(R.string.msg_playlist_failed)
            }
            actions.message(msg)
            onDone()
        }
    }

    Text(
        stringResource(R.string.menu_add_playlist),
        style = AppTheme.type.title,
        color = c.text,
        modifier = Modifier.padding(horizontal = Dimens.screenMargin, vertical = Space.s2).semantics { heading() },
    )
    MenuAction(Icons.Filled.Add, R.string.sheet_new_playlist) { creating = true }
    when (val l = load) {
        Load.Loading -> LoadingList(3)
        Load.Failed -> EmptyState(
            Icons.Filled.Warning,
            stringResource(R.string.detail_error_title),
            stringResource(R.string.search_error_hint),
            action = { RetryButton { attempt++ } },
            modifier = Modifier.fillMaxWidth(),
        )
        is Load.Ready -> if (l.value.isEmpty()) {
            EmptyState(
                Icons.Filled.LibraryMusic,
                stringResource(R.string.sheet_no_playlists_title),
                stringResource(R.string.sheet_no_playlists_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(Modifier.heightIn(max = Dimens.artHeader * 2).verticalScroll(rememberScrollState())) {
                l.value.forEach { p -> PlaylistRow(p) { addTo(p) } }
            }
        }
    }
    if (creating) {
        CreatePlaylistDialog(
            onSubmit = { name ->
                try {
                    addTo(actions.container.playlists.create(name))
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

/** S13: name a new playlist. [onSubmit] returns false on failure so the dialog can show an error and stay open. */
@Composable
fun CreatePlaylistDialog(onSubmit: suspend (String) -> Boolean, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = c.surfaceRaised,
        titleContentColor = c.text,
        textContentColor = c.text,
        title = { Text(stringResource(R.string.playlist_create_title), style = AppTheme.type.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_PLAYLIST_NAME) { name = it; failed = false } },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    isError = failed,
                    singleLine = true,
                    shape = Radius.full,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (failed) {
                    Spacer(Modifier.height(Space.s2))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.danger, modifier = Modifier.size(Space.s4))
                        Spacer(Modifier.width(Space.s1))
                        Text(stringResource(R.string.playlist_create_failed), style = AppTheme.type.caption, color = c.danger)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        val ok = onSubmit(name.trim())
                        busy = false
                        if (ok) onDismiss() else failed = true
                    }
                },
            ) { Text(stringResource(R.string.playlist_create_confirm), style = AppTheme.type.label, color = c.accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.playlist_create_cancel), style = AppTheme.type.label, color = c.textMuted)
            }
        },
    )
}
