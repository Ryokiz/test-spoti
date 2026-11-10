package com.example.app.domain

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val durationMs: Long,
    val audioUri: String,
    val explicit: Boolean = false,
)

data class Artist(val id: String, val name: String)
data class Album(val id: String, val title: String, val artistId: String, val artistName: String, val year: Int?)
data class AlbumDetail(val album: Album, val tracks: List<Track>)
data class ArtistDetail(val artist: Artist, val popular: List<Track>, val albums: List<Album>)
data class Playlist(val id: String, val name: String, val trackCount: Int)
data class PlaylistDetail(val playlist: Playlist, val tracks: List<Track>)
