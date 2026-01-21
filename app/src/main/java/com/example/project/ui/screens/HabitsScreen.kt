package com.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.project.data.model.Frequency
import com.example.project.data.model.Habit
import com.example.project.data.viewmodel.HabitViewModel
import com.example.project.data.viewmodel.UserViewModel
import com.example.project.ui.screens.components.HabitCard
import com.example.project.ui.theme.Purple
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    habitViewModel: HabitViewModel = viewModel(),
    userViewModel: UserViewModel
) {
    val habits = habitViewModel.habits
    val userName = userViewModel.username ?: "User"
    val isLoadingUser = userViewModel.isLoading

    // Add loading state for habits initial load
    var showHabitsLoading by remember { mutableStateOf(true) }

    val date = remember {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, EEEE")
        LocalDate.now().format(formatter)
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    // Show loading for a minimum time to prevent flashing
    LaunchedEffect(Unit) {
        delay(300) // Minimum loading time
        showHabitsLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 49.dp)
            ) {
                // Show loading indicator or username
                if (isLoadingUser) {
                    Box(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .height(32.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    Text(
                        text = "Welcome, $userName!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 24.dp)
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
                contentColor = MaterialTheme.colorScheme.onPrimary
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
            Spacer(modifier = Modifier.height(8.dp))

            // Show loading spinner during initial load
            if (showHabitsLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (habits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No habits yet :(",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start building better habits by pressing the Add button",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onChecked = { habitViewModel.markHabitAsDone(habit) },
                            onEdit = { editingHabit = habit },
                            onDelete = { habitViewModel.deleteHabit(habit) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (showAddDialog) {
            HabitDialog(
                title = "Create New Habit",
                onDismiss = { showAddDialog = false },
                onSave = { name, frequency ->
                    habitViewModel.addHabit(name, frequency)
                    showAddDialog = false
                }
            )
        }

        editingHabit?.let { habit ->
            HabitDialog(
                title = "Edit Habit",
                habit = habit,
                onDismiss = { editingHabit = null },
                onSave = { name, frequency ->
                    habitViewModel.editHabit(habit, name, frequency)
                    editingHabit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDialog(
    title: String,
    habit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (String, Frequency) -> Unit
) {
    var name by remember { mutableStateOf(habit?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(habit?.frequency?.getTypeName() ?: "daily") }
    var interval by remember { mutableIntStateOf(habit?.frequency?.interval ?: 1) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name cannot be empty", color = MaterialTheme.colorScheme.error) }
                    } else null
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
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Hourly", "Daily", "Weekly", "Monthly", "Yearly").forEach { option ->
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Every $interval ${Frequency.getUnitName(selectedType, interval)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row {
                        IconButton(onClick = { if (interval > 1) interval-- }) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            text = "$interval",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { if (interval < 99) interval++ }) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val frequency = Frequency.fromTypeAndInterval(selectedType, interval)
                        onSave(name, frequency)
                    } else {
                        nameError = true
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (habit == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}