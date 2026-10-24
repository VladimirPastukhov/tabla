package io.github.vladimirpastukhov.tabla

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat

/**
 * Asks for the event name and account, then creates the pinned shortcut.
 *
 * Opened by the launcher's shortcut picker (ACTION_CREATE_SHORTCUT: the shortcut is returned
 * as the activity result) or from the app drawer (the shortcut is pinned via requestPinShortcut).
 */
class SetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        val repository = CalendarRepository(contentResolver)
        setContent {
            TablaTheme {
                SetupScreen(
                    hasPermission = { hasCalendarPermission() },
                    loadCalendars = { repository.mainCalendars() },
                    onCreate = ::createShortcut,
                )
            }
        }
    }

    private fun createShortcut(shortcut: LogShortcut) {
        val info = shortcut.toShortcutInfo(this)
        if (intent.action == Intent.ACTION_CREATE_SHORTCUT) {
            setResult(Activity.RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, info))
        } else if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            ShortcutManagerCompat.requestPinShortcut(this, info, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        finish()
    }
}

@Composable
private fun SetupScreen(
    hasPermission: () -> Boolean,
    loadCalendars: () -> List<AccountCalendar>,
    onCreate: (LogShortcut) -> Unit,
) {
    var granted by remember { mutableStateOf(hasPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasPermission() }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(CALENDAR_PERMISSIONS)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
            if (granted) {
                SetupForm(calendars = remember { loadCalendars() }, onCreate = onCreate)
            } else {
                Text(stringResource(R.string.permission_needed))
                Button(onClick = { permissionLauncher.launch(CALENDAR_PERMISSIONS) }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}

@Composable
private fun SetupForm(calendars: List<AccountCalendar>, onCreate: (LogShortcut) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(calendars.singleOrNull()) }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.event_name)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    Text(stringResource(R.string.account), style = MaterialTheme.typography.titleMedium)
    if (calendars.isEmpty()) {
        Text(stringResource(R.string.no_accounts))
    }
    Column(Modifier.selectableGroup()) {
        calendars.forEach { calendar ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = calendar == selected,
                        onClick = { selected = calendar },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = calendar == selected, onClick = null)
                Text(calendar.accountName, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    val calendar = selected
    Button(
        onClick = { calendar?.let { onCreate(LogShortcut(name.trim(), it)) } },
        enabled = name.isNotBlank() && calendar != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.create))
    }
}

@Composable
private fun TablaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
