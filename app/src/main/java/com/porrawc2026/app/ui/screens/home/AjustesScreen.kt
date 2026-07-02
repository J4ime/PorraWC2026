package com.porrawc2026.app.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.util.LogManager
import com.porrawc2026.app.util.ValidationResult

@Composable
fun AjustesScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val validationResult by viewModel.validationResult.collectAsStateWithLifecycle()
    val hasData by viewModel.hasData.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdating.collectAsStateWithLifecycle()
    val appVersion by viewModel.appVersion.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importExcel(it) } }

    validationResult?.let { result ->
        ValidationDialog(result = result, onDismiss = { viewModel.dismissValidation() })
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Borrar datos", color = Color.White) },
            text = { Text("Se eliminar\u00E1n todos los datos importados del Excel. \u00BFEst\u00E1s seguro?", color = Color(0xFF999999)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))) { Text("BORRAR") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF777777))) { Text("Cancelar") }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SquareButton(
                        icon = Icons.Filled.SystemUpdate,
                        label = if (isUpdating) "Descargando" else "Actualizar",
                        color = Color(0xFF1565C0),
                        loading = isUpdating,
                        onClick = { viewModel.installUpdate() }
                    )
                }
                item {
                    SquareButton(
                            icon = Icons.Filled.GridOn,
                        label = if (hasData) "Actualizar" else "Cargar",
                        color = Color(0xFF2E7D32),
                        loading = isLoading,
                        onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }
                    )
                }
                if (hasData) {
                    item {
                        SquareButton(
                            icon = Icons.Filled.GridOn,
                            label = "Borrar",
                            color = Color(0xFFB71C1C),
                            loading = false,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
                item {
                    SquareButton(
                        icon = if (notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        label = if (notificationsEnabled) "Notif. ON" else "Notif. OFF",
                        color = if (notificationsEnabled) Color(0xFFE65100) else Color(0xFF444444),
                        loading = false,
                        onClick = { viewModel.toggleNotifications() }
                    )
                }
                item {
                    SquareButton(
                        icon = Icons.Filled.Cached,
                        label = "Reset",
                        color = Color(0xFF444444),
                        loading = isBusy,
                        onClick = {                         viewModel.clearAndRefreshCache() }
                    )
                }
                item {
                    var showLogs by remember { mutableStateOf(false) }
                    SquareButton(
                        icon = Icons.Filled.BugReport,
                        label = "Logs",
                        color = Color(0xFF444444),
                        loading = false,
                        onClick = { showLogs = true }
                    )
                    if (showLogs) {
                        LogDialog(onDismiss = { showLogs = false })
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (userName != null) {
                        Text(userName!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888), maxLines = 1)
                    }
                }
                Text("v$appVersion", style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555))
            }
        }

        if (isBusy) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing))
            )
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                Text("\u26BD", fontSize = 64.sp, color = Color.White, modifier = Modifier.rotate(rotation))
            }
        }
    }
}

@Composable
private fun SquareButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            } else {
                Icon(icon, null, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(LogManager.getLogs()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Registro de errores", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row {
                        IconButton(onClick = {
                            LogManager.clearLogs()
                            logs = ""
                        }) { Icon(Icons.Filled.Delete, "Limpiar logs", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("app_logs", logs))
                            Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Filled.ContentCopy, "Copiar logs", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { onDismiss() }) { Icon(Icons.Filled.Close, "Cerrar", tint = Color(0xFF888888), modifier = Modifier.size(20.dp)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color(0xFF0E0E0E), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isBlank()) {
                        Text(
                            "Sin errores registrados",
                            color = Color(0xFF555555),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Text(
                            text = logs,
                            color = Color(0xFFCCCCCC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationDialog(result: ValidationResult, onDismiss: () -> Unit) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    Dialog(onDismissRequest = { dismissed = true; onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (result.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = if (result.isValid) Color(0xFF4CAF50) else Color(0xFF888888), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (result.isValid) "EXCEL V\u00C1LIDO" else "EXCEL INCOMPLETO", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                if (!result.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No es v\u00E1lido. Revisa el Excel y vuelve a cargarlo.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF999999), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { dismissed = true; onDismiss() }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (result.isValid) Color(0xFF444444) else Color(0xFF333333)), shape = RoundedCornerShape(10.dp)) {
                    Text(if (result.isValid) "CONTINUAR" else "ENTENDIDO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
