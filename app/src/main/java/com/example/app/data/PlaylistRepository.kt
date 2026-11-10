package com.example.app.data

import com.example.app.domain.Playlist
import com.example.app.domain.PlaylistDetail
import com.example.app.domain.Track
import kotlinx.coroutines.delay

/** Playlists the signed-in person owns. Missing playlist: throw NoSuchElementException. */
interface PlaylistRepository {
    suspend fun list(): List<Playlist>
    suspend fun create(name: String): Playlist

    /** Returns how many tracks were actually added; tracks already in the playlist are skipped. */
    suspend fun addTracks(playlistId: String, tracks: List<Track>): Int
    suspend fun detail(id: String): PlaylistDetail
}

/** In-memory stand-in. Lost on app restart; the Supabase version persists. */
class FakePlaylistRepository : PlaylistRepository {
    private class Entry(val id: String, val name: String, val tracks: MutableList<Track> = mutableListOf())

    private val entries = mutableListOf<Entry>()
    private var next = 1

    override suspend fun list(): List<Playlist> {
        delay(150)
        return entries.map { Playlist(it.id, it.name, it.tracks.size) }
    }

    override suspend fun create(name: String): Playlist {
        delay(150)
        val clean = name.trim()
        require(clean.length in 1..100) { "name length" }
        val e = Entry("p${next++}", clean)
        entries.add(0, e)
        return Playlist(e.id, e.name, 0)
    }

    override suspend fun addTracks(playlistId: String, tracks: List<Track>): Int {
        delay(150)
        val e = entries.firstOrNull { it.id == playlistId } ?: throw NoSuchElementException("playlist")
        val fresh = tracks.distinctBy { it.id }.filter { t -> e.tracks.none { it.id == t.id } }
        e.tracks.addAll(fresh)
        return fresh.size
    }

    override suspend fun detail(id: String): PlaylistDetail {
        delay(150)
        val e = entries.firstOrNull { it.id == id } ?: throw NoSuchElementException("playlist")
        return PlaylistDetail(Playlist(e.id, e.name, e.tracks.size), e.tracks.toList())
    }
}
