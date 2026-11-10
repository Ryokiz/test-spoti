package com.example.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data object Failed : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
}

/** Runs [block] when the keys or [attempt] change. Bump [attempt] to retry. */
@Composable
fun <T> loadAsync(vararg keys: Any?, attempt: Int = 0, block: suspend () -> T): Load<T> {
    var state by remember(*keys, attempt) { mutableStateOf<Load<T>>(Load.Loading) }
    LaunchedEffect(*keys, attempt) {
        state = Load.Loading
        state = try {
            Load.Ready(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Load.Failed
        }
    }
    return state
}
