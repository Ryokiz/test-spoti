package com.example.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.app.R
import com.example.app.playback.PlayerUiState
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Space

@Composable
fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit, onToggle: () -> Unit) {
    val c = AppTheme.colors
    val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(Dimens.miniPlayerHeight).background(c.surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.player_open), onClick = onOpen)
                .padding(start = Dimens.screenMargin, end = Space.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt()
            Spacer(Modifier.width(Space.s3))
            Column(Modifier.weight(1f)) {
                Text(state.title, style = AppTheme.type.label, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, style = AppTheme.type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(Dimens.touchTarget)) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                    tint = c.text,
                )
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            color = c.accent,
            trackColor = c.border,
        )
    }
}
