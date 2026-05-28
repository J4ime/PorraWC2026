package com.porrawc2026.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.util.ValidationResult

@Composable
fun HomeScreen(
    onNavigateToGroups: () -> Unit,
    onNavigateToKnockout: () -> Unit,
    onNavigateToQuestions: () -> Unit,
    onNavigateToPlayers: () -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val hasData by viewModel.hasData.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcel(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { /* handled by snackbar */ }
    }

    validationResult?.let { result ->
        ValidationDialog(
            result = result,
            onDismiss = { viewModel.dismissValidation() }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WCDarkBlue, WCNavy, SurfaceDark)
                )
            )
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(WCBlue, WCDarkBlue, WCBlue)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PORRA",
                        style = MaterialTheme.typography.displayLarge,
                        color = WCGold,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "MUNDIAL 2026",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "USA · MÉXICO · CANADÁ",
                        style = MaterialTheme.typography.titleMedium,
                        color = WCGoldLight,
                        letterSpacing = 3.sp
                    )
                }
            }
        }

        // Points Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PUNTOS TOTALES",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalPoints",
                        style = MaterialTheme.typography.displayLarge,
                        color = WCGold,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PointsItem("Grupos", totalPoints, WCGold)
                        PointsItem("Elim.", totalPoints, AccentGreen)
                        PointsItem("Pregs.", totalPoints, AccentBlue)
                        PointsItem("Jug.", totalPoints, AccentOrange)
                    }
                }
            }
        }

        // Action buttons
        item {
            Text(
                text = "SECCIONES",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                letterSpacing = 2.sp
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionButton(
                    title = "Fase de Grupos",
                    subtitle = "Predice los resultados de 72 partidos",
                    icon = Icons.Filled.Groups,
                    color = WCGold,
                    enabled = hasData,
                    onClick = onNavigateToGroups
                )
                SectionButton(
                    title = "Eliminatorias",
                    subtitle = "Dieciseisavos a la Final",
                    icon = Icons.Filled.EmojiEvents,
                    color = WCGold,
                    enabled = hasData,
                    onClick = onNavigateToKnockout
                )
                SectionButton(
                    title = "50 Preguntas",
                    subtitle = "Verdadero o Falso · 20 pts cada una",
                    icon = Icons.Filled.Quiz,
                    color = AccentBlue,
                    enabled = hasData,
                    onClick = onNavigateToQuestions
                )
                SectionButton(
                    title = "Jugadores Favoritos",
                    subtitle = "Elige 3 goleadores · 50/30/10 pts por gol",
                    icon = Icons.Filled.Person,
                    color = AccentOrange,
                    enabled = hasData,
                    onClick = onNavigateToPlayers
                )
                SectionButton(
                    title = "Resultados en Directo",
                    subtitle = "Actualiza y consulta tus puntos",
                    icon = Icons.Filled.Update,
                    color = AccentGreen,
                    enabled = hasData,
                    onClick = onNavigateToResults
                )
            }
        }

        // Import button
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WCGold,
                        contentColor = WCDarkBlue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = WCDarkBlue,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cargar Excel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!hasData) {
                    Text(
                        text = "Carga tu Excel con las predicciones para empezar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ValidationDialog(
    result: ValidationResult,
    onDismiss: () -> Unit
) {
    var dismissed by remember { mutableStateOf(false) }

    if (dismissed) return

    Dialog(
        onDismissRequest = { dismissed = true; onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (result.isValid) AccentGreen else AccentOrange,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (result.isValid) "EXCEL VÁLIDO" else "EXCEL INCOMPLETO",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (result.isValid) AccentGreen else AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress
                val progress = if (result.totalChecks > 0) result.passedChecks.toFloat() / result.totalChecks else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (result.isValid) AccentGreen else AccentOrange,
                    trackColor = InputBg,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${result.passedChecks}/${result.totalChecks} campos completados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                // Errors
                if (result.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pendiente (${result.errors.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .heightIn(max = 250.dp)
                    ) {
                        LazyColumn {
                            items(result.errors.take(15)) { error ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", color = AccentRed, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (result.errors.size > 15) {
                                item {
                                    Text(
                                        "...y ${result.errors.size - 15} más",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { dismissed = true; onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (result.isValid) AccentGreen else WCBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (result.isValid) "CONTINUAR" else "ENTENDIDO",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!result.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Puedes completar los datos desde la app",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PointsItem(label: String, points: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = points.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun SectionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) SurfaceMedium else SurfaceMedium.copy(alpha = 0.4f)
    val contentAlpha = if (enabled) 1f else 0.4f

    Card(
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f * contentAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color.copy(alpha = contentAlpha), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary.copy(alpha = contentAlpha)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = contentAlpha)
                )
            }
            if (!enabled) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}
