package com.example.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.app.R
import com.example.app.domain.Playlist
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import com.example.app.ui.theme.Space

@Composable
fun songsLabel(count: Int): String =
    LocalContext.current.resources.getQuantityString(R.plurals.songs_count, count, count)

@Composable
fun DetailBackBar(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.padding(start = Space.s2, top = Space.s2).size(Dimens.touchTarget),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = AppTheme.colors.text)
    }
}

/** Back button, big cover, title, optional tappable subtitle and a small meta line. Shared by S06, S07, S08. */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    meta: String? = null,
    round: Boolean = false,
) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        DetailBackBar(onBack)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Dimens.screenMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoverArt(Dimens.artHeader, if (round) Radius.full else Radius.md)
            Spacer(Modifier.height(Space.s4))
            Text(
                title,
                style = AppTheme.type.headline,
                color = c.text,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                if (onSubtitleClick != null) {
                    TextButton(onClick = onSubtitleClick) {
                        Text(subtitle, style = AppTheme.type.label, color = c.accent)
                    }
                } else {
                    Text(subtitle, style = AppTheme.type.label, color = c.textMuted)
                }
            }
            if (meta != null) {
                Text(meta, style = AppTheme.type.caption, color = c.textMuted)
            }
        }
    }
}

@Composable
fun PlayRow(onPlay: () -> Unit, onShuffle: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(Dimens.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        PrimaryButton(stringResource(R.string.action_play), onPlay, Modifier.weight(1f))
        OutlinedButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f).heightIn(min = Dimens.touchTarget),
            shape = Radius.full,
            border = BorderStroke(Dimens.stroke, c.borderInput),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.accent),
        ) { Text(stringResource(R.string.action_shuffle), style = AppTheme.type.label) }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = AppTheme.type.title,
        color = AppTheme.colors.text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenMargin, vertical = Space.s2)
            .semantics { heading() },
    )
}

@Composable
fun ContentCard(title: String, subtitle: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.width(Dimens.artCard).clickable(onClick = onClick)) {
        CoverArt(Dimens.artCard, Radius.md)
        Spacer(Modifier.height(Space.s2))
        Text(title, style = AppTheme.type.label, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = AppTheme.type.caption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.miniPlayerHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.screenMargin, vertical = Space.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt()
        Spacer(Modifier.width(Space.s3))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = AppTheme.type.label, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(songsLabel(playlist.trackCount), style = AppTheme.type.caption, color = c.textMuted)
        }
    }
}
