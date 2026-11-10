package com.example.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.R
import com.example.app.domain.Track
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Space
import kotlin.random.Random

@Composable
private fun DetailLoading(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DetailBackBar(onBack)
        LoadingList()
    }
}

@Composable
private fun DetailError(onBack: () -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DetailBackBar(onBack)
        EmptyState(
            Icons.Filled.Warning,
            stringResource(R.string.detail_error_title),
            stringResource(R.string.search_error_hint),
            action = { RetryButton(onRetry) },
        )
    }
}

private fun playFrom(actions: Actions, tracks: List<Track>, index: Int) = actions.container.player.play(tracks, index)

private fun shuffleAll(actions: Actions, tracks: List<Track>) =
    actions.container.player.play(tracks, Random.nextInt(tracks.size), shuffle = true)

/** S07 */
@Composable
fun AlbumScreen(albumId: String, actions: Actions) {
    var attempt by remember { mutableIntStateOf(0) }
    val load = loadAsync(albumId, attempt = attempt) { actions.container.catalog.album(albumId) }
    val player by actions.container.player.state.collectAsStateWithLifecycle()

    when (val l = load) {
        Load.Loading -> DetailLoading(actions.back)
        Load.Failed -> DetailError(actions.back) { attempt++ }
        is Load.Ready -> {
            val d = l.value
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    DetailHeader(
                        title = d.album.title,
                        onBack = actions.back,
                        subtitle = d.album.artistName,
                        onSubtitleClick = { actions.openArtist(d.album.artistId) },
                        meta = listOfNotNull(d.album.year?.toString(), songsLabel(d.tracks.size)).joinToString(" · "),
                    )
                }
                if (d.tracks.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Filled.LibraryMusic,
                            stringResource(R.string.album_empty_title),
                            stringResource(R.string.album_empty_hint),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item { PlayRow(onPlay = { playFrom(actions, d.tracks, 0) }, onShuffle = { shuffleAll(actions, d.tracks) }) }
                    itemsIndexed(d.tracks, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, player.currentId == t.id, { playFrom(actions, d.tracks, i) }, actions, index = i)
                    }
                }
            }
        }
    }
}

/** S08. Popular is "newest first" until play counts exist; Follow is not built yet. */
@Composable
fun ArtistScreen(artistId: String, actions: Actions) {
    var attempt by remember { mutableIntStateOf(0) }
    val load = loadAsync(artistId, attempt = attempt) { actions.container.catalog.artist(artistId) }
    val player by actions.container.player.state.collectAsStateWithLifecycle()

    when (val l = load) {
        Load.Loading -> DetailLoading(actions.back)
        Load.Failed -> DetailError(actions.back) { attempt++ }
        is Load.Ready -> {
            val d = l.value
            LazyColumn(Modifier.fillMaxSize()) {
                item { DetailHeader(title = d.artist.name, onBack = actions.back, round = true) }
                if (d.popular.isEmpty() && d.albums.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Filled.LibraryMusic,
                            stringResource(R.string.album_empty_title),
                            stringResource(R.string.album_empty_hint),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (d.popular.isNotEmpty()) {
                    item { PlayRow(onPlay = { playFrom(actions, d.popular, 0) }, onShuffle = { shuffleAll(actions, d.popular) }) }
                    item { SectionTitle(stringResource(R.string.section_popular)) }
                    itemsIndexed(d.popular, key = { _, t -> t.id }) { i, t ->
                        TrackRow(t, player.currentId == t.id, { playFrom(actions, d.popular, i) }, actions, index = i)
                    }
                }
                if (d.albums.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.section_albums)) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.screenMargin),
                            horizontalArrangement = Arrangement.spacedBy(Space.s3),
                        ) {
                            items(d.albums, key = { it.id }) { a ->
                                ContentCard(a.title, a.year?.toString() ?: d.artist.name) { actions.openAlbum(a.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** S06. Rename, delete and reorder (S14) are not built yet. */
@Composable
fun PlaylistScreen(playlistId: String, actions: Actions) {
    var attempt by remember { mutableIntStateOf(0) }
    val load = loadAsync(playlistId, attempt = attempt) { actions.container.playlists.detail(playlistId) }
    val player by actions.container.player.state.collectAsStateWithLifecycle()

    when (val l = load) {
        Load.Loading -> DetailLoading(actions.back)
        Load.Failed -> DetailError(actions.back) { attempt++ }
        is Load.Ready -> {
            val d = l.value
            LazyColumn(Modifier.fillMaxSize()) {
                item { DetailHeader(title = d.playlist.name, onBack = actions.back, meta = songsLabel(d.tracks.size)) }
                if (d.tracks.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Filled.LibraryMusic,
                            stringResource(R.string.playlist_empty_title),
                            stringResource(R.string.playlist_empty_hint),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item { PlayRow(onPlay = { playFrom(actions, d.tracks, 0) }, onShuffle = { shuffleAll(actions, d.tracks) }) }
                    itemsIndexed(d.tracks, key = { i, t -> "${t.id}-$i" }) { i, t ->
                        TrackRow(t, player.currentId == t.id, { playFrom(actions, d.tracks, i) }, actions)
                    }
                }
            }
        }
    }
}
