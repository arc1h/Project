package com.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.screens.components.HabitCard
import com.example.project.ui.theme.Purple
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.foundation.layout.Row
import com.example.project.data.util.cancelHabitAlarm
import com.example.project.data.util.scheduleHabitAlarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    habitViewModel: HabitViewModel = viewModel(),
    userViewModel: UserViewModel,
    heroViewModel: HeroViewModel = viewModel()
) {
    val habits = habitViewModel.habits
    val userName = userViewModel.username ?: "User"
    val isLoadingUser = userViewModel.isLoading
    // Fix: Get Android context for the alarm manager
    val context = LocalContext.current

    var showHabitsLoading by remember { mutableStateOf(true) }
    val date = remember {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, EEEE")
        LocalDate.now().format(formatter)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    LaunchedEffect(Unit) {
        delay(300)
        showHabitsLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                if (isLoadingUser) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Welcome, $userName!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Purple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (showHabitsLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (habits.isEmpty()) {
                // Empty state UI...
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onChecked = {
                                habitViewModel.markHabitAsDone(habit)
                                rewardForHabitCompletion(habit, heroViewModel)
                            },
                            onEdit = { editingHabit = habit }, // Trigger Edit Dialog
                            onDelete = {
                                // Cancel alarm before deleting
                                cancelHabitAlarm(context, habit.name)
                                habitViewModel.deleteHabit(habit)
                            }
                        )
                    }
                }
            }
        }

        // ADD NEW HABIT DIALOG
        if (showAddDialog) {
            HabitDialog(
                title = "Create New Habit",
                onDismiss = { showAddDialog = false },
                onSave = { name, frequency, time ->
                    habitViewModel.addHabit(name, frequency)
                    if (time != "Off") {
                        val parts = time.split(":")
                        scheduleHabitAlarm(context, name, parts[0].toInt(), parts[1].toInt())
                    }
                    showAddDialog = false
                }
            )
        }

        // EDIT EXISTING HABIT DIALOG
        editingHabit?.let { habit ->
            HabitDialog(
                title = "Edit Habit",
                habit = habit,
                onDismiss = { editingHabit = null },
                onSave = { name, frequency, time ->
                    // 1. Cancel old alarm (in case name or time changed)
                    cancelHabitAlarm(context, habit.name)

                    // 2. Update Database
                    habitViewModel.editHabit(habit, name, frequency)

                    // 3. Schedule new alarm if time is set
                    if (time != "Off") {
                        val parts = time.split(":")
                        scheduleHabitAlarm(context, name, parts[0].toInt(), parts[1].toInt())
                    }
                    editingHabit = null
                }
            )
        }
    }
}

// REWARD FUNCTION
fun rewardForHabitCompletion(habit: Habit, heroViewModel: HeroViewModel) {
    val xpReward = when (habit.frequency) {
        is Frequency.Hourly -> 10
        is Frequency.Daily -> 25
        is Frequency.Weekly -> 50
        is Frequency.Monthly -> 100
    }

    val coinReward = xpReward / 2

    heroViewModel.addXP(xpReward)
    heroViewModel.addCoins(coinReward)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDialog(
    title: String,
    habit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (String, Frequency, String) -> Unit // Added String for reminderTime
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(habit?.frequency?.getTypeName() ?: "daily") }
    var interval by remember { mutableIntStateOf(habit?.frequency?.interval ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(habit?.frequency?.daysOfWeek ?: emptySet()) }

    // Reminder State
    var reminderTime by remember { mutableStateOf("Off") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = it.isBlank() },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError
                )

                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Frequency Type") },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("Hourly", "Daily", "Weekly", "Monthly").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedType = option.lowercase()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Interval Control Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Every $interval ${Frequency.getUnitName(selectedType, interval)}")
                    Row {
                        IconButton(onClick = { if (interval > 1) interval-- }) { Text("−") }
                        Text(text = "$interval", modifier = Modifier.padding(top = 12.dp))
                        IconButton(onClick = { if (interval < 99) interval++ }) { Text("+") }
                    }
                }

                if (selectedType in listOf("daily", "weekly")) {
                    Spacer(Modifier.height(16.dp))
                    DayPicker(selectedDays = selectedDays, onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                    })
                }

                // REMINDER ROW (Now correctly inside the 'text' Column)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily Reminder", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, hour, minute ->
                            reminderTime = String.format("%02d:%02d", hour, minute)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    }) {
                        Text(reminderTime, color = Purple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val frequency = Frequency.fromTypeAndInterval(
                            selectedType,
                            interval,
                            daysOfWeek = if (selectedDays.isEmpty()) null else selectedDays
                        )
                        // Pass the reminderTime back to HabitsScreen
                        onSave(name, frequency, reminderTime)
                    } else {
                        nameError = true
                    }
                }
            ) { Text(if (habit == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DayPicker(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit
) {
    // Custom sort: Sunday first
    val sundayFirst = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        sundayFirst.forEach { day ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Purple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onDaySelected(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(1),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}