package com.example.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.app.R
import com.example.app.playback.PlayerController
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import com.example.app.ui.theme.Space

@Composable
fun NowPlayingScreen(player: PlayerController, onBack: () -> Unit) {
    val s by player.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
    val shownPosition = (fraction * s.durationMs).toLong()
    val positionDescription = stringResource(R.string.position_of, formatTime(shownPosition), formatTime(s.durationMs))

    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.screenMargin)) {
        IconButton(onClick = onBack, modifier = Modifier.size(Dimens.touchTarget)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_close), tint = c.text)
        }
        Spacer(Modifier.height(Space.s4))

        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).background(c.surfaceRaised, Radius.md),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(Dimens.artHeader / 2))
        }
        Spacer(Modifier.height(Space.s6))

        Text(s.title, style = AppTheme.type.headline, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(s.artist, style = AppTheme.type.body, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)

        if (s.hasError) {
            Spacer(Modifier.height(Space.s2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = c.danger)
                Spacer(Modifier.size(Space.s2))
                Text(stringResource(R.string.player_error), style = AppTheme.type.bodySm, color = c.danger)
            }
        }

        Spacer(Modifier.height(Space.s4))
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { player.seekTo((it * s.durationMs).toLong()) }
                dragFraction = null
            },
            enabled = s.durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = c.accent,
                activeTrackColor = c.accent,
                inactiveTrackColor = c.border,
            ),
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = positionDescription },
        )
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shownPosition), style = AppTheme.type.caption, color = c.textMuted)
            Text(formatTime(s.durationMs), style = AppTheme.type.caption, color = c.textMuted)
        }

        Spacer(Modifier.height(Space.s4))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = player::toggleShuffle, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = stringResource(if (s.shuffle) R.string.player_shuffle_on else R.string.player_shuffle_off),
                    tint = if (s.shuffle) c.accent else c.textMuted,
                )
            }
            IconButton(onClick = player::previous, enabled = s.hasPrevious, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.player_previous),
                    tint = if (s.hasPrevious) c.text else c.border,
                )
            }
            FilledIconButton(
                onClick = player::togglePlay,
                modifier = Modifier.size(Dimens.playButton),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.accent, contentColor = c.onAccent),
            ) {
                if (s.isBuffering) {
                    CircularProgressIndicator(color = c.onAccent, modifier = Modifier.size(Space.s6))
                } else {
                    Icon(
                        if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (s.isPlaying) R.string.player_pause else R.string.player_play),
                    )
                }
            }
            IconButton(onClick = player::next, enabled = s.hasNext, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.player_next),
                    tint = if (s.hasNext) c.text else c.border,
                )
            }
            IconButton(onClick = player::cycleRepeat, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(
                    if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = stringResource(
                        when (s.repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.string.player_repeat_one
                            Player.REPEAT_MODE_ALL -> R.string.player_repeat_all
                            else -> R.string.player_repeat_off
                        },
                    ),
                    tint = if (s.repeatMode == Player.REPEAT_MODE_OFF) c.textMuted else c.accent,
                )
            }
        }
    }
}
