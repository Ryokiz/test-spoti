package com.example.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app.R
import com.example.app.domain.Track
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val tracks: List<Track>) : SearchState
    data object Empty : SearchState
    data object Failed : SearchState
}

@Composable
fun SearchScreen(actions: Actions) {
    val container = actions.container
    val c = AppTheme.colors
    val focus = LocalFocusManager.current
    val player by container.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, attempt) {
        if (query.isBlank()) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        state = SearchState.Loading
        delay(300) // wait for the person to stop typing
        state = try {
            val found = container.catalog.search(query)
            if (found.isEmpty()) SearchState.Empty else SearchState.Results(found)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SearchState.Failed
        }
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_label)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            singleLine = true,
            shape = Radius.full,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = appTextFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(Dimens.screenMargin),
        )

        when (val st = state) {
            SearchState.Idle -> EmptyState(
                Icons.Filled.Search,
                stringResource(R.string.search_idle_title),
                stringResource(R.string.search_idle_hint),
            )
            SearchState.Loading -> LoadingList()
            SearchState.Empty -> EmptyState(
                Icons.Filled.Search,
                stringResource(R.string.search_empty_title),
                stringResource(R.string.search_empty_hint),
            )
            SearchState.Failed -> EmptyState(
                Icons.Filled.Warning,
                stringResource(R.string.search_error_title),
                stringResource(R.string.search_error_hint),
                action = { RetryButton { attempt++ } },
            )
            is SearchState.Results -> LazyColumn {
                itemsIndexed(st.tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = player.currentId == track.id,
                        onClick = { container.player.play(st.tracks, index) },
                        actions = actions,
                    )
                }
            }
        }
    }
}
