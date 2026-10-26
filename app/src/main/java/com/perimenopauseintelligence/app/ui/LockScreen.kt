package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.UnlockResult
import com.perimenopauseintelligence.app.data.PinCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(container: AppContainer) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var forgot by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        val attempt = pin
        scope.launch {
            val result = withContext(Dispatchers.Default) { container.tryUnlock(attempt) }
            pin = ""
            message = when (result) {
                UnlockResult.Success -> null
                UnlockResult.Wrong -> "That PIN isn't right. Please try again."
                is UnlockResult.TryLater -> "Too many tries. Please wait ${result.seconds} seconds."
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        BrandHeader(logoSize = 72.dp)
        Spacer(Modifier.height(24.dp))
        Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
        Text("Enter your PIN to open your journal.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin, onValueChange = { pin = it.filter(Char::isDigit).take(PinCodec.MAX_LENGTH); message = null },
            label = { Text("PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (pin.length >= PinCodec.MIN_LENGTH) submit() }),
            isError = message != null, supportingText = { message?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { submit() }, enabled = pin.length >= PinCodec.MIN_LENGTH, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Unlock") }
        TextButton(onClick = { forgot = true }) { Text("Forgot PIN?") }
    }

    if (forgot) {
        AlertDialog(
            onDismissRequest = { forgot = false },
            title = { Text("Forgot your PIN?") },
            text = { Text("Your data is encrypted and the PIN can't be recovered. The only way back in is to erase everything on this device and start again. If you exported a copy earlier, you keep that.") },
            confirmButton = {
                TextButton(onClick = {
                    forgot = false
                    scope.launch {
                        container.repo.deleteEverything()
                        container.settings.clear()
                        container.reminders.reschedule()
                        container.unlockAfterReset()
                    }
                }) { Text("Erase and start again") }
            },
            dismissButton = { TextButton(onClick = { forgot = false }) { Text("Cancel") } },
        )
    }
}
