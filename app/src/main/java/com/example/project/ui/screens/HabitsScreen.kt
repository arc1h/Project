package com.example.project.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.model.HabitDifficulty
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.data.viewmodel.HeroViewModel
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.screens.components.HabitCard
import com.example.project.ui.theme.Purple
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    val context = LocalContext.current

    var showHabitsLoading by remember { mutableStateOf(true) }
    val date = remember {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, EEEE")
        LocalDate.now().format(formatter)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    val listState = rememberLazyListState()
    val shouldHideFab by remember {
        derivedStateOf {
            // 1. If the list is too short to even scroll, always show the FAB
            val canScroll = listState.canScrollForward || listState.canScrollBackward
            if (!canScroll) return@derivedStateOf false

            // 2. Identify scroll direction and position
            val isScrollingDown = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            val isAtBottom = !listState.canScrollForward

            // 3. Only hide if actively moving down OR at the very bottom of a long list
            val isMovingDown = listState.isScrollInProgress &&
                    listState.firstVisibleItemIndex >= (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0)

            (isAtBottom || isMovingDown) && isScrollingDown
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        showHabitsLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Spacer(Modifier.height(12.dp))
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
            val visible = habits.isEmpty() || !shouldHideFab
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Purple,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit")
                }
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
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No habits.. :(",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Click the + button to add one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onChecked = {
                                habitViewModel.markHabitAsDone(habit)

                                // 1. General Progress (Consistency Star)
                                heroViewModel.incrementChallengeProgress("habit_completed")

                                // 2. Early Bird Check (Before 11 AM)
                                val calendar = Calendar.getInstance()
                                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                                if (currentHour < 11) {
                                    heroViewModel.incrementChallengeProgress("morning_habit")
                                }

                                // 3. Hard Mode Check
                                if (habit.difficulty == HabitDifficulty.HARD) {
                                    heroViewModel.incrementChallengeProgress("hard_habit")
                                }

                                // 4. Weekend Warrior Check (Sunday)
                                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                    heroViewModel.incrementChallengeProgress("weekend")
                                }

                                // 5. Night Owl Check (After 8 PM/20:00)
                                if (currentHour >= 20) {
                                    heroViewModel.incrementChallengeProgress("night_owl")
                                }

                                // 6. Habit Streak Check
                                // Increment progress if the *new* streak contributes up to the goal of 3.
                                // If they do streak 1 -> streak 2 -> streak 3, it increments 3 times (completing challenge).
                                // Future completions (streak 4+) won't erroneously increment it again.
                                if (habit.streak + 1 <= 3) {
                                    heroViewModel.incrementChallengeProgress("streak")
                                }
                            },
                            onEdit = { editingHabit = habit },
                            onDelete = { habitViewModel.deleteHabit(context, habit) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            HabitDialog(
                title = "Create New Habit",
                onDismiss = { showAddDialog = false },
                onSave = { name, frequency, difficulty, time ->
                    habitViewModel.addHabit(context, name, frequency, difficulty, time)
                    showAddDialog = false
                }
            )
        }

        editingHabit?.let { habit ->
            HabitDialog(
                title = "Edit Habit",
                habit = habit,
                onDismiss = { editingHabit = null },
                onSave = { name, frequency, difficulty, time ->
                    habitViewModel.editHabit(context, habit, name, frequency, difficulty, time)
                    editingHabit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDialog(
    title: String,
    habit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (String, Frequency, HabitDifficulty, String) -> Unit
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(habit?.frequency?.getTypeName() ?: "daily") }
    var interval by remember { mutableIntStateOf(habit?.frequency?.interval ?: 1) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(habit?.frequency?.daysOfWeek ?: emptySet()) }

    // Reminder State
    var reminderTime by remember { mutableStateOf(habit?.reminderTime ?: "Off") }
    val context = LocalContext.current

    var selectedDifficulty by remember { mutableStateOf(habit?.difficulty ?: HabitDifficulty.EASY) }

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
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth(),
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
                Text("Difficulty", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HabitDifficulty.entries.forEach { difficulty -> // Use .entries for modern Kotlin
                        FilterChip(
                            selected = selectedDifficulty == difficulty,
                            onClick = { selectedDifficulty = difficulty },
                            label = { Text(difficulty.toDisplayString()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedType in listOf("daily", "weekly")) {
                    Spacer(Modifier.height(16.dp))
                    DayPicker(selectedDays = selectedDays, onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                    })
                }
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
                        onSave(name, frequency, selectedDifficulty, reminderTime)
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