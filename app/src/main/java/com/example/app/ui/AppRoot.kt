package com.example.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app.AppContainer
import com.example.app.R
import com.example.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val NOW_PLAYING = "now_playing"
    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"
    const val PLAYLIST = "playlist/{id}"

    fun album(id: String) = "album/${Uri.encode(id)}"
    fun artist(id: String) = "artist/${Uri.encode(id)}"
    fun playlist(id: String) = "playlist/${Uri.encode(id)}"
}

private data class NavItem(val route: String, val label: Int, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    NavItem(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search),
    NavItem(Routes.LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic),
)

@Composable
fun AppRoot(container: AppContainer) {
    val signedIn by container.auth.signedIn.collectAsStateWithLifecycle()
    if (signedIn) MainShell(container) else AuthScreen(container.auth)
}

@Composable
private fun MainShell(container: AppContainer) {
    val nav = rememberNavController()
    val player by container.player.state.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val inPlayer = route == Routes.NOW_PLAYING
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val actions = remember(container, nav) {
        Actions(
            container = container,
            openAlbum = { nav.navigate(Routes.album(it)) },
            openArtist = { nav.navigate(Routes.artist(it)) },
            openPlaylist = { nav.navigate(Routes.playlist(it)) },
            back = { nav.popBackStack() },
            message = { text ->
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(text)
                }
            },
        )
    }

    RequestNotificationPermission()

    Scaffold(
        containerColor = AppTheme.colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!inPlayer) {
                Column {
                    if (player.hasItem) {
                        MiniPlayer(
                            state = player,
                            onOpen = { nav.navigate(Routes.NOW_PLAYING) },
                            onToggle = container.player::togglePlay,
                        )
                    }
                    BottomNav(current = route) { target ->
                        nav.navigate(target) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { padding ->
        val idArg = listOf(navArgument("id") { type = NavType.StringType })
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(actions) }
            composable(Routes.SEARCH) { SearchScreen(actions) }
            composable(Routes.LIBRARY) { LibraryScreen(actions) }
            composable(Routes.ALBUM, arguments = idArg) { e -> AlbumScreen(e.arguments?.getString("id").orEmpty(), actions) }
            composable(Routes.ARTIST, arguments = idArg) { e -> ArtistScreen(e.arguments?.getString("id").orEmpty(), actions) }
            composable(Routes.PLAYLIST, arguments = idArg) { e -> PlaylistScreen(e.arguments?.getString("id").orEmpty(), actions) }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(container.player, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomNav(current: String?, onSelect: (String) -> Unit) {
    val c = AppTheme.colors
    NavigationBar(containerColor = c.surface) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = current == item.route,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.label), style = AppTheme.type.caption) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.accent,
                    selectedTextColor = c.accent,
                    indicatorColor = c.surfaceRaised,
                    unselectedIconColor = c.textMuted,
                    unselectedTextColor = c.textMuted,
                ),
            )
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
