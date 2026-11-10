package com.example.app.data

import android.content.Context
import com.example.app.domain.Album
import com.example.app.domain.AlbumDetail
import com.example.app.domain.Artist
import com.example.app.domain.ArtistDetail
import com.example.app.domain.Track
import kotlinx.coroutines.delay

/**
 * The data layer the screens talk to. FakeCatalogRepository serves seed data; the Supabase-backed version
 * keeps these signatures so no screen changes. Missing album or artist: throw NoSuchElementException.
 */
interface CatalogRepository {
    suspend fun search(query: String): List<Track>
    suspend fun allTracks(): List<Track>
    suspend fun album(id: String): AlbumDetail
    suspend fun artist(id: String): ArtistDetail
}

class FakeCatalogRepository(context: Context) : CatalogRepository {
    private val pkg = context.packageName
    private fun raw(name: String) = "android.resource://$pkg/raw/$name"

    private val artists = listOf(Artist("a1", "Sample Artist"), Artist("a2", "Another Sample Artist"))
    private val albums = listOf(
        Album("b1", "Test Pressings", "a1", "Sample Artist", 2026),
        Album("b2", "Second Pressings", "a2", "Another Sample Artist", 2026),
    )

    private fun track(id: String, title: String, album: Album, file: String) = Track(
        id = id, title = title,
        artist = album.artistName, artistId = album.artistId,
        album = album.title, albumId = album.id,
        durationMs = 12_000, audioUri = raw(file),
    )

    private val tracks = listOf(
        track("t1", "Morning Arpeggio", albums[0], "morning_arpeggio"),
        track("t2", "Slow Waves", albums[0], "slow_waves"),
        track("t3", "Night Steps", albums[1], "night_steps"),
    )

    override suspend fun allTracks(): List<Track> {
        delay(150)
        return tracks
    }

    override suspend fun search(query: String): List<Track> {
        delay(150)
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return tracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }

    override suspend fun album(id: String): AlbumDetail {
        delay(150)
        val album = albums.firstOrNull { it.id == id } ?: throw NoSuchElementException("album")
        return AlbumDetail(album, tracks.filter { it.albumId == id })
    }

    override suspend fun artist(id: String): ArtistDetail {
        delay(150)
        val artist = artists.firstOrNull { it.id == id } ?: throw NoSuchElementException("artist")
        return ArtistDetail(artist, tracks.filter { it.artistId == id }.take(5), albums.filter { it.artistId == id })
    }
}
