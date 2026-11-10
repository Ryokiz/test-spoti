package com.example.app.data.supabase

import com.example.app.domain.Album
import com.example.app.domain.Track
import org.json.JSONArray
import org.json.JSONObject

/** PostgREST select for a track with its album and artist embedded. */
const val TRACK_SELECT = "id,title,duration_ms,explicit,album_id,albums(id,title,primary_artist_id,artists(id,name))"

fun trackFromRow(r: JSONObject): Track {
    val album = r.getJSONObject("albums")
    val artist = album.getJSONObject("artists")
    val id = r.getString("id")
    return Track(
        id = id,
        title = r.getString("title"),
        artist = artist.getString("name"),
        artistId = artist.getString("id"),
        album = album.getString("title"),
        albumId = album.getString("id"),
        durationMs = r.getLong("duration_ms"),
        audioUri = "track://$id",
        explicit = r.getBoolean("explicit"),
    )
}

fun tracksFromJson(text: String): List<Track> {
    val rows = JSONArray(text)
    return List(rows.length()) { trackFromRow(rows.getJSONObject(it)) }
}

/** Expects an album row that embeds its artist as `artists(id,name)`. */
fun albumFromRow(r: JSONObject): Album {
    val artist = r.getJSONObject("artists")
    return Album(
        id = r.getString("id"),
        title = r.getString("title"),
        artistId = artist.getString("id"),
        artistName = artist.getString("name"),
        year = r.optString("release_date").take(4).toIntOrNull(),
    )
}
