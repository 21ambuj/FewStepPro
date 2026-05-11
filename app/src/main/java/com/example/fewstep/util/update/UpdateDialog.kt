package com.example.fewstep.util.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf(DownloadState.IDLE) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadId by remember { mutableStateOf(-1L) }

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress,
        animationSpec = tween(300),
        label = "progress"
    )

    // Poll download progress while downloading
    LaunchedEffect(downloadId) {
        if (downloadId == -1L) return@LaunchedEffect
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (downloadState == DownloadState.DOWNLOADING) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query)
            if (cursor.moveToFirst()) {
                val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L
                val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else 0

                if (total > 0) {
                    downloadProgress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                }
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloadState = DownloadState.COMPLETE
                        downloadProgress = 1f
                    }
                    DownloadManager.STATUS_FAILED -> {
                        downloadState = DownloadState.FAILED
                    }
                }
            }
            cursor.close()
            delay(300)
        }
    }

    // Auto-install when download completes
    LaunchedEffect(downloadState) {
        if (downloadState == DownloadState.COMPLETE) {
            delay(500)
            installApk(context, updateInfo.apkUrl)
        }
    }

    Dialog(
        onDismissRequest = { if (!updateInfo.isForceUpdate && downloadState == DownloadState.IDLE) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.isForceUpdate,
            dismissOnClickOutside = !updateInfo.isForceUpdate && downloadState == DownloadState.IDLE
        )
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated icon header
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "✨ Update Available!",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Version ${updateInfo.latestVersionName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                // Release notes card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = updateInfo.releaseNotes,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Progress Section
                when (downloadState) {
                    DownloadState.DOWNLOADING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Downloading... ${(animatedProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    DownloadState.COMPLETE -> {
                        Text(
                            "✅ Download complete! Installing...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF43A047)
                        )
                    }
                    DownloadState.FAILED -> {
                        Text(
                            "❌ Download failed. Please try again.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    DownloadState.IDLE -> {
                        // Buttons
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!updateInfo.isForceUpdate) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Later")
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        downloadState = DownloadState.DOWNLOADING
                                        downloadId = startDownload(context, updateInfo.apkUrl)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Update Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DownloadState { IDLE, DOWNLOADING, COMPLETE, FAILED }

private fun startDownload(context: Context, apkUrl: String): Long {
    return try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("FewStep Update")
            setDescription("Downloading latest version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            val destDir = File(context.externalCacheDir, "updates").apply { mkdirs() }
            val destFile = File(destDir, "fewstep_update.apk")
            setDestinationUri(Uri.fromFile(destFile))
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
        }
        dm.enqueue(request)
    } catch (e: Exception) {
        android.util.Log.e("UpdateDialog", "Failed to start download: ${e.message}")
        -1L
    }
}

private fun installApk(context: Context, apkUrl: String) {
    try {
        val apkFile = File(context.externalCacheDir, "updates/fewstep_update.apk")
        if (!apkFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        android.util.Log.e("UpdateDialog", "Failed to install APK: ${e.message}")
    }
}
