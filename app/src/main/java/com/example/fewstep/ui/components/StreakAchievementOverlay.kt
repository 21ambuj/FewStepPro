package com.example.fewstep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable

@Composable
fun StreakAchievementOverlay(streak: Int, isMilestone: Boolean = false, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_infinite")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val animState = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        animState.animateTo(1f, animationSpec = tween(600, easing = { fraction ->
            val tension = 1.2f
            var t = fraction - 1.0f
            t * t * ((tension + 1) * t + tension) + 1.0f
        }))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E1E),
                        Color(0xFF000000)
                    )
                )
            )
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        ConfettiEffect()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer(
                    scaleX = animState.value,
                    scaleY = animState.value,
                    alpha = animState.value
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isMilestone) {
                Surface(
                    color = Color(0xFFFFD600).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "🏆 NEW MILESTONE",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD600),
                        letterSpacing = 2.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 1.1f
                    )
                }
                
                Icon(
                    imageVector = if (isMilestone) Icons.Default.EmojiEvents else Icons.Default.Whatshot,
                    contentDescription = null,
                    modifier = Modifier.size(160.dp),
                    tint = if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isMilestone) "$streak DAYS!" else "$streak DAY STREAK!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            val subtitle = when (streak) {
                365 -> "1 FULL YEAR! You have truly mastered this habit! 🏆🌟"
                200 -> "200 DAYS! Absolute legend status achieved! 👑⚡"
                100 -> "100 DAYS! A century of dedication and pure fire! 🔥💯"
                50 -> "50 DAYS! Halfway to a century! Keep pushing! 🚀"
                30 -> "30 DAYS! A full month of consistency! Beast mode! 🐺"
                7 -> "7 DAYS! One full week down. The habit is taking root! 🌱"
                else -> "$streak days of pure fire! Keep the momentum going! 🔥"
            }

            Text(
                text = subtitle,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (isMilestone) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = Color(0xFFFFD600).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600))
                ) {
                    Text(
                        "+${streak * 10} BONUS XP RECEIVED 🌟",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color(0xFFFFD600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMilestone) Color(0xFFFFD600) else Color(0xFFFF5722)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = if (isMilestone) "CLAIM REWARD 🏆" else "KEEP CLIMBING 🚀",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isMilestone) Color.Black else Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            TextButton(
                onClick = {
                    com.example.fewstep.util.ShareUtils.shareMilestone(context, streak, isMilestone)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "SHARE ACHIEVEMENT ✨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Close Icon at Top Right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ConfettiEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "y_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val colors = listOf(Color.Yellow, Color.Cyan, Color.Magenta, Color.Green, Color.Red)
        val random = java.util.Random(42)
        repeat(30) {
            val x = random.nextFloat() * size.width
            val initialY = random.nextFloat() * size.height
            val currentY = (initialY + yOffset) % size.height
            val color = colors[random.nextInt(colors.size)]
            val shapeSize = 8.dp.toPx()
            
            drawRect(
                color = color.copy(alpha = 0.6f),
                topLeft = androidx.compose.ui.geometry.Offset(x, currentY),
                size = androidx.compose.ui.geometry.Size(shapeSize, shapeSize)
            )
        }
    }
}
