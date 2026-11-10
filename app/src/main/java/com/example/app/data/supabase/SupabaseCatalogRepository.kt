package com.example.app.data.supabase

import android.net.Uri
import com.example.app.data.CatalogRepository
import com.example.app.domain.AlbumDetail
import com.example.app.domain.Artist
import com.example.app.domain.ArtistDetail
import com.example.app.domain.Track
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tracks carry a `track://<id>` URI, not a file URL: StreamUrlResolver swaps in a short-lived signed URL
 * each time playback opens the file, so rights and tier are checked by the server every time.
 * Every id placed in a URL goes through Uri.encode so it cannot add filters to the query.
 */
class SupabaseCatalogRepository(private val http: SupabaseHttp) : CatalogRepository {

    override suspend fun search(query: String): List<Track> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val rows = JSONArray(http.callAsync("POST", "/rest/v1/rpc/search_catalog", JSONObject().put("q", q).put("lim", 30)))
        return List(rows.length()) { i ->
            val r = rows.getJSONObject(i)
            val id = r.getString("track_id")
            Track(
                id = id,
                title = r.getString("title"),
                artist = r.getString("artist"),
                artistId = r.getString("artist_id"),
                album = r.getString("album"),
                albumId = r.getString("album_id"),
                durationMs = r.getLong("duration_ms"),
                audioUri = "track://$id",
                explicit = r.getBoolean("explicit"),
            )
        }
    }

    override suspend fun allTracks(): List<Track> =
        tracksFromJson(http.callAsync("GET", "/rest/v1/tracks?select=$TRACK_SELECT&order=created_at.desc&limit=50"))

    override suspend fun album(id: String): AlbumDetail {
        val rows = JSONArray(
            http.callAsync(
                "GET",
                "/rest/v1/albums?id=eq.${Uri.encode(id)}" +
                    "&select=id,title,release_date,primary_artist_id,artists(id,name)," +
                    "tracks(id,title,duration_ms,explicit,track_number,disc_number)" +
                    "&tracks.order=disc_number.asc,track_number.asc",
            ),
        )
        if (rows.length() == 0) throw NoSuchElementException("album")
        val row = rows.getJSONObject(0)
        val album = albumFromRow(row)
        val list = row.getJSONArray("tracks")
        val tracks = List(list.length()) { i ->
            val t = list.getJSONObject(i)
            val trackId = t.getString("id")
            Track(
                id = trackId,
                title = t.getString("title"),
                artist = album.artistName,
                artistId = album.artistId,
                album = album.title,
                albumId = album.id,
                durationMs = t.getLong("duration_ms"),
                audioUri = "track://$trackId",
                explicit = t.getBoolean("explicit"),
            )
        }
        return AlbumDetail(album, tracks)
    }

    override suspend fun artist(id: String): ArtistDetail {
        val enc = Uri.encode(id)
        val artists = JSONArray(http.callAsync("GET", "/rest/v1/artists?id=eq.$enc&select=id,name"))
        if (artists.length() == 0) throw NoSuchElementException("artist")
        val a = artists.getJSONObject(0)
        val albums = JSONArray(
            http.callAsync("GET", "/rest/v1/albums?primary_artist_id=eq.$enc&select=id,title,release_date,artists(id,name)&order=release_date.desc"),
        )
        val popular = tracksFromJson(
            http.callAsync(
                "GET",
                "/rest/v1/tracks?select=${TRACK_SELECT.replace("albums(", "albums!inner(")}" +
                    "&albums.primary_artist_id=eq.$enc&order=created_at.desc&limit=5",
            ),
        )
        return ArtistDetail(
            Artist(a.getString("id"), a.getString("name")),
            popular,
            List(albums.length()) { albumFromRow(albums.getJSONObject(it)) },
        )
    }
}
