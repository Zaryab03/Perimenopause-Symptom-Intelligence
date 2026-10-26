package com.perimenopauseintelligence.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.data.DefaultSymptoms
import com.perimenopauseintelligence.app.data.PeriodStatus
import com.perimenopauseintelligence.app.data.PinCodec
import com.perimenopauseintelligence.app.domain.SymptomInfo
import kotlinx.coroutines.launch

private const val STEPS = 6

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val allSymptoms by container.repo.observeSymptoms().collectAsStateWithLifecycle(emptyList())
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(PeriodStatus.SKIP) }
    var chosen by remember { mutableStateOf(DefaultSymptoms.starterIds) }
    var remind by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(20) }
    var minute by remember { mutableIntStateOf(0) }
    var pickTime by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { remind = it }

    fun finish() {
        container.settings.setUserName(name)
        container.settings.setPeriodStatus(period)
        container.settings.reminderEnabled = remind
        container.settings.reminderHour = hour
        container.settings.reminderMinute = minute
        container.reminders.reschedule()
        if (PinCodec.isValid(pin) && pin == pin2) container.settings.setPin(pin)
        scope.launch {
            container.repo.setVisibleSymptoms(chosen)
            container.settings.setOnboardingDone(true)
        }
    }

    val pinOk = pin.isEmpty() || (PinCodec.isValid(pin) && pin == pin2)

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(STEPS) { i ->
                Box(
                    Modifier.padding(3.dp).size(if (i == step) 22.dp else 8.dp, 8.dp)
                        .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BrandHeader() }
                    Text("Perimenopause Symptom Intelligence", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(
                        "Understand your symptom history, explore your personal patterns, and prepare for more informed conversations with your healthcare professional.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("Private by design", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Everything stays on your phone, encrypted. No account needed, no ads, no tracking. This app doesn't diagnose or give medical advice.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                1 -> {
                    Text("What should we call you?", style = MaterialTheme.typography.headlineSmall)
                    Text("Just a first name or nickname, used for greetings on this phone. It's optional and never leaves your device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                2 -> {
                    Text("Do you still have periods?", style = MaterialTheme.typography.headlineSmall)
                    Text("This only decides whether we show the optional bleeding question. You can change it any time.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PeriodStatus.entries.forEach { p ->
                        AppCard(
                            onClick = { period = p },
                            color = if (period == p) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Text(p.label, style = MaterialTheme.typography.titleSmall)
                            Text(p.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                3 -> {
                    Text("What would you like to keep track of?", style = MaterialTheme.typography.headlineSmall)
                    Text("Choose what appears in your daily log. We've picked some common ones. You can add or hide more any time.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SymptomChooser(allSymptoms, chosen) { chosen = it }
                }
                4 -> {
                    Text("A gentle reminder?", style = MaterialTheme.typography.headlineSmall)
                    Text("One optional notification a day at a time you choose. It never mentions symptoms, and missing a day is completely fine.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Remind me daily", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Switch(remind, { on -> if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else remind = on })
                        }
                        if (remind) OutlinedButton(onClick = { pickTime = true }) { Text("Time: %02d:%02d".format(hour, minute)) }
                    }
                }
                5 -> {
                    Text("Protect it with a PIN?", style = MaterialTheme.typography.headlineSmall)
                    Text("Optional. With a PIN, the app locks itself shortly after you leave it, and hides its contents from screenshots and the recent-apps view.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(PinCodec.MAX_LENGTH) }, label = { Text("PIN (${PinCodec.MIN_LENGTH}–${PinCodec.MAX_LENGTH} digits)") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                    if (pin.isNotEmpty()) {
                        OutlinedTextField(pin2, { pin2 = it.filter(Char::isDigit).take(PinCodec.MAX_LENGTH) }, label = { Text("Repeat PIN") },
                            singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(),
                            isError = pin2.isNotEmpty() && pin != pin2, supportingText = { if (pin2.isNotEmpty() && pin != pin2) Text("PINs don't match") })
                        Text("There is no way to recover a forgotten PIN: resetting it erases the data on this device. Export a copy now and then.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("Back") } else Spacer(Modifier.size(1.dp))
            Spacer(Modifier.weight(1f))
            if (step in listOf(1, 4, 5)) {
                TextButton(onClick = { if (step == STEPS - 1) { pin = ""; pin2 = ""; finish() } else step++ }) { Text("Skip") }
            }
            Button(
                onClick = { if (step == STEPS - 1) finish() else step++ },
                enabled = step != 5 || pinOk,
                modifier = Modifier.heightIn48(),
            ) { Text(if (step == 0) "Get started" else if (step == STEPS - 1) "Finish" else "Next") }
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(hour, minute, is24Hour = android.text.format.DateFormat.is24HourFormat(ctx))
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { hour = state.hour; minute = state.minute; pickTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
        )
    }
}

private fun Modifier.heightIn48(): Modifier = this.then(Modifier.height(48.dp))

/** Grouped multi-select of symptoms. Used in onboarding and in "Add or hide symptoms". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomChooser(all: List<SymptomInfo>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    val byCategory = all.groupBy { it.category }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        byCategory.forEach { (category, list) ->
            Text("${DefaultSymptoms.emojiFor(category)}  $category", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { s ->
                    Pill(
                        text = s.label, selected = s.id in selected,
                        onClick = { onChange(if (s.id in selected) selected - s.id else selected + s.id) },
                        selectedColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedContent = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
