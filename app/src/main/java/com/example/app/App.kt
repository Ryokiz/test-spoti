package com.example.app

import android.app.Application
import android.content.Context
import com.example.app.data.AuthRepository
import com.example.app.data.CatalogRepository
import com.example.app.data.FakeAuthRepository
import com.example.app.data.supabase.SessionStore
import com.example.app.data.supabase.SupabaseAuth
import com.example.app.data.supabase.SupabaseCatalogRepository
import com.example.app.data.supabase.SupabaseHttp
import com.example.app.data.supabase.SupabasePlaylistRepository
import com.example.app.data.FakeCatalogRepository
import com.example.app.data.FakePlaylistRepository
import com.example.app.data.PlaylistRepository
import com.example.app.playback.PlayerController

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Manual dependency container. With supabase.url and supabase.anon.key set in local.properties the real
 * clients are used; otherwise the fakes, so the app still runs with no backend.
 */
class AppContainer(context: Context) {
    val http: SupabaseHttp? =
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            SupabaseHttp(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, SessionStore(context))
        } else {
            null
        }
    val auth: AuthRepository = http?.let { SupabaseAuth(it) } ?: FakeAuthRepository(context)
    val catalog: CatalogRepository = http?.let { SupabaseCatalogRepository(it) } ?: FakeCatalogRepository(context)
    val playlists: PlaylistRepository = http?.let { SupabasePlaylistRepository(it) } ?: FakePlaylistRepository()
    val player = PlayerController(context)
}
