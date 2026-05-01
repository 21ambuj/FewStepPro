package com.example.fewstep.ui.screens.habit

import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fewstep.ui.viewmodel.HomeViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Health") }
    var customCategory by remember { mutableStateOf("") }
    var isCustomCategorySelected by remember { mutableStateOf(false) }
    
    var isDurationEnabled by remember { mutableStateOf(false) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    
    var selectedStartDateText by remember { mutableStateOf("") }
    var selectedEndDateText by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var selectedDays by remember { mutableStateOf(listOf(1, 2, 3, 4, 5, 6, 7)) }

    val daysOfWeek = listOf(
        "S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7
    )

    val startDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            startDateMillis = cal.timeInMillis
            selectedStartDateText = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            endDateMillis = cal.timeInMillis
            selectedEndDateText = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val amPm = if (hourOfDay < 12) "AM" else "PM"
            val formattedHour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
            val formattedMinute = String.format("%02d", minute)
            selectedTime = "$formattedHour:$formattedMinute $amPm"
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add New Habit", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit Title") },
                placeholder = { Text("e.g., Drink Water") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Category", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Health", "Education", "Fitness", "Custom").forEach { cat ->
                    FilterChip(
                        selected = if (cat == "Custom") isCustomCategorySelected else (category == cat && !isCustomCategorySelected),
                        onClick = {
                            if (cat == "Custom") {
                                isCustomCategorySelected = true
                            } else {
                                isCustomCategorySelected = false
                                category = cat
                            }
                        },
                        label = { Text(cat) }
                    )
                }
            }

            if (isCustomCategorySelected) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { customCategory = it },
                    label = { Text("Custom Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { 
                    val words = it.trim().split(Regex("\\s+")).filter { s -> s.isNotEmpty() }
                    if (words.size <= 20) description = it 
                },
                label = { Text("Short Description (Max 20 words)") },
                placeholder = { Text("e.g. Focus on deep breathing") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = { 
                    val count = description.trim().split(Regex("\\s+")).filter { s -> s.isNotEmpty() }.size
                    Text("$count/20 words") 
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { (label, value) ->
                    val isSelected = selectedDays.contains(value)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - value else selectedDays
                                } else {
                                    (selectedDays + value).sorted()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set Mission Duration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Switch(
                    checked = isDurationEnabled,
                    onCheckedChange = { isDurationEnabled = it }
                )
            }

            if (isDurationEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = selectedStartDateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f).clickable { startDatePicker.show() },
                        trailingIcon = {
                            IconButton(onClick = { startDatePicker.show() }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = selectedEndDateText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End") },
                        modifier = Modifier.weight(1f).clickable { endDatePicker.show() },
                        trailingIcon = {
                            IconButton(onClick = { endDatePicker.show() }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = selectedTime,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reminder Time") },
                trailingIcon = {
                    IconButton(onClick = { timePickerDialog.show() }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Select Time")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { timePickerDialog.show() },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val finalCategory = if (isCustomCategorySelected) customCategory else category
                    when {
                        title.isEmpty() -> Toast.makeText(context, "Enter title!", Toast.LENGTH_SHORT).show()
                        isCustomCategorySelected && customCategory.isEmpty() -> Toast.makeText(context, "Enter custom category!", Toast.LENGTH_SHORT).show()
                        isDurationEnabled && (startDateMillis == null || endDateMillis == null) -> Toast.makeText(context, "Set both dates!", Toast.LENGTH_SHORT).show()
                        selectedTime.isEmpty() -> Toast.makeText(context, "Select time!", Toast.LENGTH_SHORT).show()
                        else -> {
                            viewModel.addHabit(
                                context = context,
                                title = title,
                                description = description,
                                category = finalCategory,
                                frequency = if (selectedDays.size == 7) "Daily" else "Custom",
                                scheduledDays = selectedDays,
                                reminderTime = selectedTime,
                                startDate = if (isDurationEnabled) startDateMillis else null,
                                endDate = if (isDurationEnabled) endDateMillis else null
                            )
                            Toast.makeText(context, "Mission Created Successfully! 🚀", Toast.LENGTH_LONG).show()
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create Habit", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
