package com.example.app.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.app.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val hasItem: Boolean = false,
    val currentId: String = "",
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

/** UI-facing wrapper around a MediaController connected to PlaybackService. */
class PlayerController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var pending: ((MediaController) -> Unit)? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (player.isPlaying) startTicker() else ticker?.cancel()
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                publish(c)
                pending?.invoke(c)
                pending = null
            } catch (e: Exception) {
                _state.value = _state.value.copy(hasError = true)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending = block
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                controller?.let { publish(it) }
                delay(500)
            }
        }
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerUiState(
            hasItem = item != null,
            currentId = item?.mediaId ?: "",
            title = item?.mediaMetadata?.title?.toString() ?: "",
            artist = item?.mediaMetadata?.artist?.toString() ?: "",
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasError = player.playerError != null,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    private fun toItem(t: Track) = MediaItem.Builder()
        .setMediaId(t.id)
        .setUri(Uri.parse(t.audioUri))
        .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build())
        .build()

    /** [shuffle] null leaves the current shuffle setting alone. */
    fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean? = null) = withController { c ->
        if (shuffle != null) c.shuffleModeEnabled = shuffle
        c.setMediaItems(tracks.map { toItem(it) }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun addToQueue(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) {
            c.setMediaItem(toItem(track))
            c.prepare()
            c.play()
        } else {
            c.addMediaItem(toItem(track))
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    /** Used on sign-out so the previous person's queue does not carry over. */
    fun stop() = withController {
        it.stop()
        it.clearMediaItems()
    }

    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
