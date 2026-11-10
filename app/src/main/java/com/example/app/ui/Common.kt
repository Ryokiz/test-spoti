package com.example.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.app.R
import com.example.app.domain.Track
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import com.example.app.ui.theme.Space
import java.util.Locale

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60)
}

@Composable
fun CoverArt(size: Dp = Dimens.artRow, shape: Shape = Radius.sm) {
    Box(
        Modifier.size(size).background(AppTheme.colors.surfaceRaised, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AppTheme.colors.textMuted)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier.padding(Space.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = AppTheme.colors.textMuted, modifier = Modifier.size(Dimens.artRow))
        Spacer(Modifier.height(Space.s4))
        Text(title, style = AppTheme.type.title, color = AppTheme.colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.s2))
        Text(hint, style = AppTheme.type.bodySm, color = AppTheme.colors.textMuted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(Space.s4))
            action()
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val c = AppTheme.colors
    val busy = stringResource(R.string.loading)
    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier.heightIn(min = Dimens.touchTarget).semantics { if (loading) stateDescription = busy },
        shape = Radius.full,
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
    ) {
        if (loading) {
            CircularProgressIndicator(color = c.onAccent, modifier = Modifier.size(Space.s6))
        } else {
            Text(text, style = AppTheme.type.label)
        }
    }
}

@Composable
fun RetryButton(onClick: () -> Unit) = PrimaryButton(stringResource(R.string.action_retry), onClick)

@Composable
fun appTextFieldColors(): TextFieldColors {
    val c = AppTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.borderInput,
        errorBorderColor = c.danger,
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
        errorTextColor = c.text,
        focusedLabelColor = c.accent,
        unfocusedLabelColor = c.textMuted,
        errorLabelColor = c.danger,
        focusedLeadingIconColor = c.accent,
        unfocusedLeadingIconColor = c.textMuted,
        focusedTrailingIconColor = c.textMuted,
        unfocusedTrailingIconColor = c.textMuted,
        errorTrailingIconColor = c.textMuted,
        cursorColor = c.accent,
        errorCursorColor = c.danger,
    )
}

/** Static placeholder rows (no shimmer, so nothing animates for reduced-motion users). */
@Composable
fun LoadingList(rows: Int = 5) {
    val label = stringResource(R.string.loading)
    Column(Modifier.semantics { contentDescription = label }) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dimens.screenMargin, vertical = Space.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt()
                Spacer(Modifier.width(Space.s3))
                Column {
                    Box(Modifier.fillMaxWidth(0.6f).height(Space.s4).background(AppTheme.colors.surfaceRaised, Radius.sm))
                    Spacer(Modifier.height(Space.s2))
                    Box(Modifier.fillMaxWidth(0.4f).height(Space.s3).background(AppTheme.colors.surfaceRaised, Radius.sm))
                }
            }
        }
    }
}
