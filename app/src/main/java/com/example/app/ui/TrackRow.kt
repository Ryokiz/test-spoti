package com.example.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.app.R
import com.example.app.domain.Track
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Space

/**
 * One song in a list. Tapping plays it; the overflow button opens the song menu (S15).
 * Pass [index] (0-based) on album pages to show a track number instead of cover art.
 */
@Composable
fun TrackRow(track: Track, isCurrent: Boolean, onClick: () -> Unit, actions: Actions, index: Int? = null) {
    val c = AppTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.miniPlayerHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.screenMargin, vertical = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null) {
            Box(Modifier.width(Dimens.artRow), contentAlignment = Alignment.Center) {
                Text((index + 1).toString(), style = AppTheme.type.bodySm, color = if (isCurrent) c.accent else c.textMuted, textAlign = TextAlign.Center)
            }
        } else {
            CoverArt()
        }
        Spacer(Modifier.width(Space.s3))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = AppTheme.type.label,
                color = if (isCurrent) c.accent else c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = AppTheme.type.bodySm,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(Dimens.touchTarget)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.track_more, track.title),
                tint = c.textMuted,
            )
        }
    }
    if (menuOpen) TrackMenuSheet(track, actions) { menuOpen = false }
}
