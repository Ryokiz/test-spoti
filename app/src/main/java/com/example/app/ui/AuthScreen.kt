package com.example.app.ui

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.app.R
import com.example.app.data.AuthFailure
import com.example.app.data.AuthRepository
import com.example.app.data.AuthResult
import com.example.app.ui.theme.AppTheme
import com.example.app.ui.theme.Dimens
import com.example.app.ui.theme.Radius
import com.example.app.ui.theme.Space
import kotlinx.coroutines.launch

private enum class AuthMode { SignIn, SignUp }

private const val MIN_PASSWORD = 8

@StringRes
private fun failureText(f: AuthFailure): Int = when (f) {
    AuthFailure.InvalidCredentials -> R.string.auth_err_invalid_credentials
    AuthFailure.EmailTaken -> R.string.auth_err_email_taken
    AuthFailure.WeakPassword -> R.string.auth_err_weak_password
    AuthFailure.Network -> R.string.auth_err_network
    AuthFailure.Unknown -> R.string.auth_err_unknown
}

/** S01: sign in, create account, and the "check your email" state. */
@Composable
fun AuthScreen(auth: AuthRepository) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current

    var mode by rememberSaveable { mutableStateOf(AuthMode.SignIn) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }          // not saved: keep secrets out of saved state
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<Int?>(null) }
    var passwordError by remember { mutableStateOf<Int?>(null) }
    var formError by remember { mutableStateOf<Int?>(null) }
    var confirmEmail by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        val trimmed = email.trim()
        emailError = if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) null else R.string.auth_err_email_invalid
        passwordError = when {
            password.isEmpty() -> R.string.auth_err_password_empty
            mode == AuthMode.SignUp && password.length < MIN_PASSWORD -> R.string.auth_err_password_short
            else -> null
        }
        formError = null
        if (emailError != null || passwordError != null) return
        loading = true
        scope.launch {
            val result = if (mode == AuthMode.SignIn) auth.signIn(trimmed, password) else auth.signUp(trimmed, password)
            when (result) {
                AuthResult.Success -> Unit                    // signedIn flips and this screen leaves
                AuthResult.ConfirmEmail -> confirmEmail = trimmed
                is AuthResult.Failure -> formError = failureText(result.reason)
            }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        val pending = confirmEmail
        if (pending != null) {
            EmptyState(
                icon = Icons.Filled.Email,
                title = stringResource(R.string.auth_confirm_title),
                hint = stringResource(R.string.auth_confirm_body, pending),
                action = {
                    PrimaryButton(stringResource(R.string.auth_confirm_back), {
                        confirmEmail = null
                        mode = AuthMode.SignIn
                        password = ""
                    })
                },
            )
            return@Box
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.screenMargin),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(if (mode == AuthMode.SignIn) R.string.auth_title_sign_in else R.string.auth_title_sign_up),
                style = AppTheme.type.headline,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Space.s2))
            Text(
                stringResource(if (mode == AuthMode.SignIn) R.string.auth_hint_sign_in else R.string.auth_hint_sign_up),
                style = AppTheme.type.body,
                color = c.textMuted,
            )
            Spacer(Modifier.height(Space.s6))

            val eErr = emailError
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text(stringResource(R.string.auth_email)) },
                isError = eErr != null,
                supportingText = if (eErr != null) { { FieldError(eErr) } } else null,
                singleLine = true,
                shape = Radius.full,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                colors = appTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.s3))

            val pErr = passwordError
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = { Text(stringResource(R.string.auth_password)) },
                isError = pErr != null,
                supportingText = if (pErr != null) { { FieldError(pErr) } } else null,
                singleLine = true,
                shape = Radius.full,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (showPassword) R.string.auth_password_hide else R.string.auth_password_show,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); submit() }),
                colors = appTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            val fErr = formError
            if (fErr != null) {
                Spacer(Modifier.height(Space.s3))
                Row(
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = c.danger, modifier = Modifier.size(Space.s6))
                    Spacer(Modifier.width(Space.s2))
                    Text(stringResource(fErr), style = AppTheme.type.bodySm, color = c.danger)
                }
            }

            Spacer(Modifier.height(Space.s6))
            PrimaryButton(
                text = stringResource(if (mode == AuthMode.SignIn) R.string.auth_submit_sign_in else R.string.auth_submit_sign_up),
                onClick = ::submit,
                loading = loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.s2))
            TextButton(
                onClick = {
                    mode = if (mode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn
                    emailError = null
                    passwordError = null
                    formError = null
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.touchTarget),
            ) {
                Text(
                    stringResource(if (mode == AuthMode.SignIn) R.string.auth_switch_to_sign_up else R.string.auth_switch_to_sign_in),
                    style = AppTheme.type.label,
                    color = c.accent,
                )
            }
        }
    }
}

@Composable
private fun FieldError(@StringRes id: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = AppTheme.colors.danger, modifier = Modifier.size(Space.s4))
        Spacer(Modifier.width(Space.s1))
        Text(stringResource(id), style = AppTheme.type.caption, color = AppTheme.colors.danger)
    }
}
