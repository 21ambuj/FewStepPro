package com.example.fewstep.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Button
import com.example.fewstep.R
import com.example.fewstep.data.model.Habit
import com.example.fewstep.data.model.HabitLog
import com.example.fewstep.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            provideContent {
                AuthRequiredContent()
            }
            return
        }

        try {
            val uid = currentUser.uid
            // Fetch Streak
            val userDoc = firestore.collection("users").document(uid).get().await()
            val streak = userDoc.getLong("currentStreak")?.toInt() ?: 0
            
            // Fetch Habits for today
            val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val todayStartMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEndMs = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val habitsSnapshot = firestore.collection("users").document(uid).collection("habits").get().await()
            val habitsForToday = habitsSnapshot.documents.mapNotNull { it.toObject(Habit::class.java) }
                .filter { habit -> 
                    habit.scheduledDays.contains(todayDayOfWeek) &&
                    (habit.startDate == null || habit.startDate <= todayEndMs) &&
                    (habit.endDate == null || todayStartMs <= habit.endDate)
                }
            
            val totalGoals = habitsForToday.size
            
            // Fetch Completions for today
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val logsSnapshot = firestore.collection("users").document(uid).collection("logs")
                .whereEqualTo("date", todayDateStr)
                .whereEqualTo("completed", true)
                .get().await()
            
            val completedGoals = logsSnapshot.size()
            val remainingGoals = (totalGoals - completedGoals).coerceAtLeast(0)
            val progressPercent = if (totalGoals > 0) ((completedGoals.toFloat() / totalGoals.toFloat()) * 100).toInt().coerceAtMost(100) else 0

            provideContent {
                HabitWidgetContent(
                    streak = streak,
                    completed = completedGoals,
                    total = totalGoals,
                    remaining = remainingGoals,
                    progress = progressPercent
                )
            }
        } catch (e: Exception) {
            provideContent {
                ErrorContent(e.message ?: "Unknown Error")
            }
        }
    }

    @Composable
    private fun AuthRequiredContent() {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1A237E))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FewStep",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Please login to the app to see your progress.",
                style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 12.sp, textAlign = androidx.glance.text.TextAlign.Center)
            )
        }
    }

    @Composable
    private fun ErrorContent(error: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1A237E))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Error loading data", style = TextStyle(color = ColorProvider(Color.Red), fontWeight = FontWeight.Bold))
            Text(text = error, style = TextStyle(color = ColorProvider(Color.White), fontSize = 10.sp))
            Button(text = "Retry", onClick = actionRunCallback<RefreshActionCallback>())
        }
    }

    @Composable
    private fun HabitWidgetContent(
        streak: Int,
        completed: Int,
        total: Int,
        remaining: Int,
        progress: Int
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1A237E)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FewStep",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "🔥 $streak",
                    style = TextStyle(color = ColorProvider(Color(0xFFFF5722)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            Text(
                text = "$progress%",
                style = TextStyle(color = ColorProvider(Color(0xFFFFC107)), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Today's Progress",
                style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 10.sp)
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                StatItem(label = "Done", value = completed.toString(), color = Color(0xFF43A047))
                Spacer(modifier = GlanceModifier.width(16.dp))
                StatItem(label = "Left", value = remaining.toString(), color = Color(0xFFE91E63))
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            Button(
                text = "Refresh",
                onClick = actionRunCallback<RefreshActionCallback>()
            )
        }
    }

    @Composable
    private fun StatItem(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = TextStyle(color = ColorProvider(color), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 9.sp)
            )
        }
    }
}

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        HabitWidget().update(context, glanceId)
    }
}
