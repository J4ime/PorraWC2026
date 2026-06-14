package com.porrawc2026.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.util.ValidationResult

@Composable
fun AjustesScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()
    val excelFileName by viewModel.excelFileName.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val autoRefreshEnabled by viewModel.autoRefreshEnabled.collectAsState()
    val pdfResult by viewModel.pdfResult.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userPosition by viewModel.userPosition.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importExcel(it) } }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.loadPdfResult(it) } }

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
                        icon = if (autoRefreshEnabled) Icons.Filled.Sync else Icons.Filled.SyncDisabled,
                        label = if (autoRefreshEnabled) "Live ON" else "Live OFF",
                        color = if (autoRefreshEnabled) Color(0xFF4CAF50) else Color(0xFF444444),
                        loading = false,
                        onClick = { viewModel.toggleAutoRefresh() }
                    )
                }
                item {
                    SquareButton(
                        icon = Icons.Filled.Cached,
                        label = "Cache",
                        color = Color(0xFF444444),
                        loading = false,
                        onClick = { viewModel.refreshCache() }
                    )
                }
                item {
                    SquareButton(
                        icon = Icons.Filled.Assessment,
                        label = when {
                            pdfResult != null -> "Pos: $pdfResult"
                            userPosition != null -> "Pos: $userPosition"
                            else -> "Cargar result"
                        },
                        color = if (userPosition != null) Color(0xFF1565C0) else Color(0xFF444444),
                        loading = false,
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (userName != null) {
                        Text(userName!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888), maxLines = 1)
                    }
                    Text(excelFileName ?: "", style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555), maxLines = 1)
                }
                Text("v$appVersion", style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555))
            }
        }

        if (isBusy) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                Text("\u26BD", fontSize = 64.sp, color = Color.White)
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
