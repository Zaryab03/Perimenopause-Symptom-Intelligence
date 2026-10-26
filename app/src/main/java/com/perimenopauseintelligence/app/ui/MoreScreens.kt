package com.perimenopauseintelligence.app.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.perimenopauseintelligence.app.data.PeriodStatus
import com.perimenopauseintelligence.app.data.PinCodec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perimenopauseintelligence.app.AppContainer
import com.perimenopauseintelligence.app.data.DefaultSymptoms
import com.perimenopauseintelligence.app.domain.Exporters
import com.perimenopauseintelligence.app.domain.ReportBuilder
import com.perimenopauseintelligence.app.domain.ReportSpec
import com.perimenopauseintelligence.app.share.ExportFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private data class MoreItem(val route: String, val emoji: String, val title: String, val subtitle: String)

private val moreItems = listOf(
    MoreItem("appointment", "\uD83D\uDCCB", "Appointment preparation", "Choose symptoms, add questions, create a PDF"),
    MoreItem("export", "\uD83D\uDCE4", "Export my data", "CSV, JSON or PDF for any date range"),
    MoreItem("symptoms", "\uD83C\uDF3F", "My symptoms", "Choose what appears in your daily log"),
    MoreItem("reminders", "\uD83D\uDD14", "Reminders", "Optional, and you choose the time"),
    MoreItem("security", "\uD83D\uDD12", "PIN and app lock", "Keep your journal private on this phone"),
    MoreItem("privacy", "\uD83D\uDEE1\uFE0F", "Privacy and my data", "How your information is stored; delete everything"),
)

@Composable
fun MoreScreen(container: AppContainer, onOpen: (String) -> Unit) {
    val name by container.settings.userName.collectAsStateWithLifecycle()
    val pinOn by container.settings.pinEnabled.collectAsStateWithLifecycle()
    ScreenList("More") {
        item {
            AppCard(onClick = { onOpen("profile") }, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.uppercase() ?: "\u2728", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (name.isBlank()) "Add your name" else name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Your profile and preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        if (pinOn) item { Button(onClick = { container.lockNow() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Lock the app now") } }
        items(moreItems.size, key = { moreItems[it].route }) { i ->
            val m = moreItems[i]
            AppCard(onClick = { onOpen(m.route) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EmojiBadge(m.emoji)
                    Column(Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.titleSmall)
                        Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text(
                "Perimenopause Symptom Intelligence \u00b7 test version ${com.perimenopauseintelligence.app.BuildConfig.VERSION_NAME}. This app helps you record and review your own " +
                    "information. It does not diagnose conditions or provide medical advice.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ProfileScreen(container: AppContainer, onBack: () -> Unit) {
    val name by container.settings.userName.collectAsStateWithLifecycle()
    val period by container.settings.periodStatus.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(name) }
    ScreenList("Your profile", onBack) {
        item { InfoCard { Text("Stored only on this phone. We don't ask for your gender, age or location, because the app doesn't need them.") } }
        item {
            OutlinedTextField(draft, { draft = it.take(40); container.settings.setUserName(it) }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { SectionHeader("Do you still have periods?") }
        items(PeriodStatus.entries.size) { i ->
            val p = PeriodStatus.entries[i]
            AppCard(onClick = { container.settings.setPeriodStatus(p) }, color = if (period == p) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                Text(p.label, style = MaterialTheme.typography.titleSmall)
                Text(p.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SecurityScreen(container: AppContainer, onBack: () -> Unit) {
    val pinOn by container.settings.pinEnabled.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    ScreenList("PIN and app lock", onBack) {
        item {
            InfoCard {
                Text("With a PIN, the app locks itself 30 seconds after you leave it. Its contents are also hidden from screenshots and the recent-apps view.")
                Text("There is no way to recover a forgotten PIN. Resetting it erases the data on this device, so export a copy now and then.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!pinOn) {
            item { Button(onClick = { dialog = "set" }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Set a PIN") } }
        } else {
            item { Button(onClick = { container.lockNow() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Lock the app now") } }
            item { OutlinedButton(onClick = { dialog = "change" }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Change PIN") } }
            item { OutlinedButton(onClick = { dialog = "remove" }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Turn PIN off") } }
        }
    }
    dialog?.let { mode -> PinDialog(container, mode) { dialog = null } }
}

@Composable
private fun PinDialog(container: AppContainer, mode: String, onClose: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val needsCurrent = mode != "set"
    val needsNew = mode != "remove"
    fun field(value: String, set: (String) -> Unit, label: String) = @Composable {
        OutlinedTextField(value, { set(it.filter(Char::isDigit).take(PinCodec.MAX_LENGTH)); error = null }, label = { Text(label) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(when (mode) { "set" -> "Set a PIN"; "change" -> "Change PIN"; else -> "Turn PIN off" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (needsCurrent) field(current, { current = it }, "Current PIN")()
                if (needsNew) {
                    field(pin, { pin = it }, "New PIN (${PinCodec.MIN_LENGTH}\u2013${PinCodec.MAX_LENGTH} digits)")()
                    field(pin2, { pin2 = it }, "Repeat new PIN")()
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (needsCurrent && !container.settings.verifyPin(current)) { error = "Current PIN isn't right."; return@TextButton }
                if (needsNew) {
                    if (!PinCodec.isValid(pin)) { error = "Use ${PinCodec.MIN_LENGTH}\u2013${PinCodec.MAX_LENGTH} digits."; return@TextButton }
                    if (pin != pin2) { error = "The PINs don't match."; return@TextButton }
                    container.settings.setPin(pin)
                } else container.settings.clearPin()
                onClose()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}

private enum class RangePreset(val label: String, val days: Int?) {
    D30("Last 30 days", 30), D90("Last 90 days", 90), Y1("Last year", 365), ALL("All time", null), CUSTOM("Custom", -1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangePicker(container: AppContainer, onChange: (LocalDate, LocalDate) -> Unit) {
    var preset by remember { mutableStateOf(RangePreset.D30) }
    var from by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun apply(p: RangePreset) {
        preset = p
        val today = LocalDate.now()
        when {
            p == RangePreset.ALL -> scope.launch {
                from = container.repo.firstEntryDate() ?: today
                to = today
                onChange(from, to)
            }
            p.days != null && p.days > 0 -> { from = today.minusDays(p.days - 1L); to = today; onChange(from, to) }
            else -> onChange(from, to)
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { onChange(from, to) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Date range", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangePreset.entries.forEach { p -> FilterChip(selected = preset == p, onClick = { apply(p) }, label = { Text(p.label) }) }
        }
        if (preset == RangePreset.CUSTOM) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickFrom = true }) { Text("From ${from.format(shortDate)}") }
                OutlinedButton(onClick = { pickTo = true }) { Text("To ${to.format(shortDate)}") }
            }
        } else {
            Text("${from.format(shortDate)} to ${to.format(shortDate)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (pickFrom) DatePickerButtonDialog(from, onPicked = { from = it; if (to.isBefore(it)) to = it; onChange(from, to); pickFrom = false }, onDismiss = { pickFrom = false })
    if (pickTo) DatePickerButtonDialog(to, onPicked = { to = it; if (from.isAfter(it)) from = it; onChange(from, to); pickTo = false }, onDismiss = { pickTo = false })
}

private suspend fun shareText(context: Context, name: String, mime: String, content: String) = withContext(Dispatchers.IO) {
    ExportFiles.shareIntent(context, ExportFiles.writeText(context, name, content), mime)
}

@Composable
fun ExportScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var from by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var includeNotes by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }

    ScreenList("Export my data", onBack) {
        item { InfoCard { Text("Your data is yours. Exports are always free. Files are created temporarily on this device and only leave it when you choose where to send them.") } }
        item { RangePicker(container) { f, t -> from = f; to = t } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Include my notes", Modifier.weight(1f))
                Switch(checked = includeNotes, onCheckedChange = { includeNotes = it })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val recs = container.repo.records(from, to)
                        val syms = container.repo.symptoms().associateBy { it.id }
                        val intent = shareText(context, "symptom-history.csv", "text/csv", Exporters.csv(recs, syms, includeNotes))
                        context.startActivity(intent); status = "CSV created: ${recs.size} days."
                    }
                }) { Text("Share CSV (spreadsheet)") }
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val recs = container.repo.records(from, to)
                        val syms = container.repo.symptoms().associateBy { it.id }
                        val intent = shareText(context, "symptom-history.json", "application/json", Exporters.json(recs, syms, from, to, LocalDate.now(), includeNotes))
                        context.startActivity(intent); status = "JSON created: ${recs.size} days."
                    }
                }) { Text("Share JSON (complete data)") }
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val recs = container.repo.records(from, to)
                        val syms = container.repo.symptoms().associateBy { it.id }
                        val blocks = ReportBuilder.build(ReportSpec("Symptom history report", from, to, includeNotes = includeNotes), syms, recs, LocalDate.now())
                        val intent = withContext(Dispatchers.IO) { ExportFiles.shareIntent(context, ExportFiles.writePdf(context, "symptom-report.pdf", blocks), "application/pdf") }
                        context.startActivity(intent); status = "PDF created."
                    }
                }) { Text("Share PDF report") }
            }
        }
        status?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
    }
}

@Composable
fun AppointmentScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val symptoms by container.repo.observeSymptoms().collectAsStateWithLifecycle(emptyList())
    val questions by container.repo.observeQuestions().collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf(setOf<String>()) }
    var from by remember { mutableStateOf(LocalDate.now().minusDays(29)) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var newQuestion by remember { mutableStateOf("") }
    var includeNotes by remember { mutableStateOf(true) }

    ScreenList("Appointment preparation", onBack) {
        item { InfoCard { Text("Pick the period and symptoms you want to talk about, add your questions, then create a PDF. You decide whether to share it.") } }
        item { RangePicker(container) { f, t -> from = f; to = t } }
        item { Text("Symptoms to include (none selected = all you recorded)", style = MaterialTheme.typography.titleSmall) }
        items(symptoms.filter { !it.archived }.size, key = { symptoms.filter { s -> !s.archived }[it].id }) { i ->
            val s = symptoms.filter { !it.archived }[i]
            Row(Modifier.fillMaxWidth().clickable { selected = if (s.id in selected) selected - s.id else selected + s.id }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = s.id in selected, onCheckedChange = { selected = if (it) selected + s.id else selected - s.id })
                Text(s.label)
            }
        }
        item { Text("Questions for my clinician", style = MaterialTheme.typography.titleSmall) }
        items(questions.size, key = { questions[it].id }) { i ->
            val q = questions[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(q.text, Modifier.weight(1f))
                TextButton(onClick = { scope.launch { container.repo.deleteQuestion(q.id) } }) { Text("Remove") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newQuestion, { newQuestion = it }, label = { Text("Add a question") }, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { scope.launch { container.repo.addQuestion(newQuestion); newQuestion = "" } }, enabled = newQuestion.isNotBlank()) { Text("Add") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Include my notes in the report", Modifier.weight(1f))
                Switch(checked = includeNotes, onCheckedChange = { includeNotes = it })
            }
        }
        item {
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    val recs = container.repo.records(from, to)
                    val syms = container.repo.symptoms().associateBy { it.id }
                    val spec = ReportSpec(
                        title = "Appointment summary", start = from, end = to,
                        symptomIds = selected.ifEmpty { null }, includeNotes = includeNotes,
                        questions = container.repo.questions(),
                    )
                    val blocks = ReportBuilder.build(spec, syms, recs, LocalDate.now())
                    val intent = withContext(Dispatchers.IO) { ExportFiles.shareIntent(context, ExportFiles.writePdf(context, "appointment-summary.pdf", blocks), "application/pdf") }
                    context.startActivity(intent)
                }
            }) { Text("Create appointment PDF") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomsScreen(container: AppContainer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val symptoms by container.repo.observeSymptoms().collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }

    ScreenList("My symptoms", onBack) {
        item { InfoCard { Text("Turn off anything you don't want to see in your daily log. Hiding a symptom never deletes what you already recorded.") } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add your own symptom", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DefaultSymptoms.categories.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
                }
                Button(onClick = { scope.launch { container.repo.addCustomSymptom(name, category); name = "" } }, enabled = name.isNotBlank()) { Text("Add symptom") }
            }
        }
        items(symptoms.size, key = { symptoms[it].id }) { i ->
            val s = symptoms[i]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.label)
                    Text(if (s.isCustom) "${s.category} · yours" else s.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = !s.archived, onCheckedChange = { scope.launch { container.repo.setSymptomArchived(s.id, !it) } })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(container: AppContainer, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(container.settings.reminderEnabled) }
    var hour by remember { mutableStateOf(container.settings.reminderHour) }
    var minute by remember { mutableStateOf(container.settings.reminderMinute) }
    var picking by remember { mutableStateOf(false) }

    fun commit(on: Boolean) {
        enabled = on
        container.settings.reminderEnabled = on
        container.settings.reminderHour = hour
        container.settings.reminderMinute = minute
        container.reminders.reschedule()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> commit(granted) }

    ScreenList("Reminders", onBack) {
        item { InfoCard { Text("Reminders are optional, once a day at most, and never mention your symptoms. Missing a day is completely fine. You can turn them off at any time.") } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily check-in reminder", Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else commit(on)
                })
            }
        }
        item { OutlinedButton(onClick = { picking = true }) { Text("Time: %02d:%02d".format(hour, minute)) } }
    }
    if (picking) {
        val state = rememberTimePickerState(hour, minute, is24Hour = android.text.format.DateFormat.is24HourFormat(ctx))
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { hour = state.hour; minute = state.minute; picking = false; commit(enabled) }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun PrivacyScreen(container: AppContainer, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    ScreenList("Privacy and my data", onBack) {
        item {
            InfoCard {
                Text("How your information is handled in this test build", style = MaterialTheme.typography.titleSmall)
                Text(
                    "• Everything you record is stored only on this device, in an encrypted database. The key is protected by the Android Keystore.\n" +
                        "• There is no account and the app has no internet permission, so nothing is sent anywhere.\n" +
                        "• There is no analytics, advertising or tracking.\n" +
                        "• Cloud and device-to-device backup are turned off for this app. If you uninstall the app or lose the device, the data is gone, so export a copy if you want to keep one.\n" +
                        "• Data leaves the app only when you export or share it. Anyone you send it to can see it.\n" +
                        "• Anyone who can unlock your phone and open this app can see your entries.",
                )
            }
        }
        item {
            InfoCard {
                Text("Not yet decided", style = MaterialTheme.typography.titleSmall)
                Text("This is a test build. A full privacy notice, accounts and cross-device sync will come later and will need legal review before any public release.")
            }
        }
        item {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { confirm = true }) { Text("Delete all my data") }
        }
        if (done) item { Text("All data was deleted.") }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Delete everything?") },
            text = { Text("This permanently deletes every entry, note, question and custom symptom on this device. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { container.repo.deleteEverything(); container.settings.clear(); container.reminders.reschedule(); done = true }; confirm = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}
