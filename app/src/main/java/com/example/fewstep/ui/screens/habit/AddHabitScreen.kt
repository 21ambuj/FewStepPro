package com.example.fewstep.ui.screens.habit

import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    var category by remember { mutableStateOf("Health") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var selectedDays by remember { mutableStateOf(listOf(1, 2, 3, 4, 5, 6, 7)) }

    val daysOfWeek = listOf(
        "S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> selectedDate = "$dayOfMonth/${month + 1}/$year" },
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
        topBar = {
            TopAppBar(
                title = { Text("Add New Habit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
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

            Text("Category", fontWeight = FontWeight.Medium, color = Color.Gray)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                listOf("Health", "Education", "Fitness").forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Schedule", fontWeight = FontWeight.Medium, color = Color.Gray)
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
                                color = if (isSelected) Color(0xFF1A237E) else Color(0xFFF5F5F5),
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
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(12.dp)
            )

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
                    when {
                        title.isEmpty() -> {
                            Toast.makeText(context, "Please enter a habit title!", Toast.LENGTH_SHORT).show()
                        }
                        selectedDate.isEmpty() -> {
                            Toast.makeText(context, "Please select a start date!", Toast.LENGTH_SHORT).show()
                        }
                        selectedTime.isEmpty() -> {
                            Toast.makeText(context, "Please select a reminder time!", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            viewModel.addHabit(
                                context = context,
                                title = title,
                                category = category,
                                frequency = if (selectedDays.size == 7) "Daily" else "Custom",
                                scheduledDays = selectedDays,
                                reminderTime = selectedTime
                            )
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E))
            ) {
                Text("Create Habit", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
