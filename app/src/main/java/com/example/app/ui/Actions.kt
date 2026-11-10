package com.example.app.ui

import com.example.app.AppContainer

/** What screens may do beyond drawing themselves: navigate, show a toast, reach the data layer and player. */
class Actions(
    val container: AppContainer,
    val openAlbum: (String) -> Unit,
    val openArtist: (String) -> Unit,
    val openPlaylist: (String) -> Unit,
    val back: () -> Unit,
    val message: (String) -> Unit,
)
