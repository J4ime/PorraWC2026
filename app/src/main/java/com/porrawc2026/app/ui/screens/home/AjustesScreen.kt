package com.porrawc2026.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val appVersion by viewModel.appVersion.collectAsState()

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
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("v$appVersion", style = MaterialTheme.typography.titleMedium, color = Color(0xFF555555), textAlign = TextAlign.Center)

            if (updateAvailable) {
                Button(
                    onClick = { viewModel.installUpdate() },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("DESCARGANDO...", style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                    } else {
                        Text("ACTUALIZAR APP", style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                    }
                }
            }

            Button(
                onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(if (hasData) Icons.Filled.Refresh else Icons.Filled.FileUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasData) "Actualizar Porra" else "Cargar Porra", style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                }
            }

            if (hasData) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A1A), contentColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar datos", style = MaterialTheme.typography.titleSmall, color = Color(0xFFE53935), maxLines = 1, softWrap = false)
                }
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
