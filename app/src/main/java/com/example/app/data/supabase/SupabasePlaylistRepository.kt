package com.example.app.data.supabase

import android.net.Uri
import com.example.app.data.PlaylistRepository
import com.example.app.domain.Playlist
import com.example.app.domain.PlaylistDetail
import com.example.app.domain.Track
import org.json.JSONArray
import org.json.JSONObject

class SupabasePlaylistRepository(private val http: SupabaseHttp) : PlaylistRepository {
    private fun userId() = http.session.userId ?: throw IllegalStateException("not signed in")

    override suspend fun list(): List<Playlist> {
        val rows = JSONArray(
            http.callAsync("GET", "/rest/v1/playlists?select=id,name,playlist_items(count)&owner_id=eq.${Uri.encode(userId())}&order=updated_at.desc"),
        )
        return List(rows.length()) { i ->
            val r = rows.getJSONObject(i)
            Playlist(r.getString("id"), r.getString("name"), r.optJSONArray("playlist_items")?.optJSONObject(0)?.optInt("count") ?: 0)
        }
    }

    override suspend fun create(name: String): Playlist {
        val body = JSONObject().put("owner_id", userId()).put("name", name.trim())
        val rows = JSONArray(http.callAsync("POST", "/rest/v1/playlists", body, headers = mapOf("Prefer" to "return=representation")))
        val r = rows.getJSONObject(0)
        return Playlist(r.getString("id"), r.getString("name"), 0)
    }

    override suspend fun addTracks(playlistId: String, tracks: List<Track>): Int {
        val ids = JSONArray()
        tracks.forEach { ids.put(it.id) }
        val body = JSONObject().put("p_playlist", playlistId).put("p_tracks", ids).put("p_skip_existing", true)
        return JSONArray(http.callAsync("POST", "/rest/v1/rpc/add_playlist_items", body)).length()
    }

    override suspend fun detail(id: String): PlaylistDetail {
        val rows = JSONArray(
            http.callAsync(
                "GET",
                "/rest/v1/playlists?id=eq.${Uri.encode(id)}&select=id,name,playlist_items(position,tracks($TRACK_SELECT))" +
                    "&playlist_items.order=position.asc",
            ),
        )
        if (rows.length() == 0) throw NoSuchElementException("playlist")
        val r = rows.getJSONObject(0)
        val items = r.getJSONArray("playlist_items")
        // A track hidden by rights (RLS) embeds as null: skip it rather than fail the whole playlist.
        val tracks = (0 until items.length()).mapNotNull { i ->
            items.getJSONObject(i).optJSONObject("tracks")?.let { trackFromRow(it) }
        }
        return PlaylistDetail(Playlist(r.getString("id"), r.getString("name"), tracks.size), tracks)
    }
}
