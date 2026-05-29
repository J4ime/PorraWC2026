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
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcel(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { /* handled by snackbar */ }
    }

    validationResult?.let { result ->
        ValidationDialog(result = result, onDismiss = { viewModel.dismissValidation() })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(WCDarkBlue, WCNavy, SurfaceDark)))
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(WCBlue, WCDarkBlue, WCBlue)))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PORRA", style = MaterialTheme.typography.displayLarge, color = WCGold, fontWeight = FontWeight.Black)
                    Text("MUNDIAL 2026", style = MaterialTheme.typography.displayMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("USA · MÉXICO · CANADÁ", style = MaterialTheme.typography.titleMedium, color = WCGoldLight, letterSpacing = 3.sp)
                }
            }
        }

        // Cargar Excel button
        item {
            Button(
                onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WCGold, contentColor = WCDarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = WCDarkBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar Excel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Upcoming matches
        if (upcomingMatches.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = WCGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (upcomingMatches.firstOrNull()?.dateTime?.let {
                                        try { val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US); sdf.parse(it) } catch (e: Exception) { null }
                                    }?.let { val c = java.util.Calendar.getInstance(); val d = java.util.Calendar.getInstance().apply { time = it }; c.get(java.util.Calendar.DAY_OF_YEAR) == d.get(java.util.Calendar.DAY_OF_YEAR) } == true) "PARTIDOS DE HOY" else "PRÓXIMA JORNADA",
                                style = MaterialTheme.typography.titleMedium,
                                color = WCGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                upcomingMatches.firstOrNull()?.dateTime?.take(10) ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        upcomingMatches.take(6).forEach { match ->
                            MatchRow(match)
                        }
                    }
                }
            }
        }

        // Points Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PUNTOS TOTALES", style = MaterialTheme.typography.labelMedium, color = TextMuted, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$totalPoints", style = MaterialTheme.typography.displayLarge, color = WCGold, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PointsItem("Grupos", totalPoints, WCGold)
                        PointsItem("Elim.", totalPoints, AccentGreen)
                        PointsItem("Pregs.", totalPoints, AccentBlue)
                        PointsItem("Jug.", totalPoints, AccentOrange)
                    }
                }
            }
        }

        // Sections
        item {
            Text("SECCIONES", style = MaterialTheme.typography.labelMedium, color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), letterSpacing = 2.sp)
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionButton("Fase de Grupos", "Predicciones de 72 partidos", Icons.Filled.Groups, WCGold, hasData, onNavigateToGroups)
                SectionButton("Eliminatorias", "Dieciseisavos a la Final", Icons.Filled.EmojiEvents, WCGold, hasData, onNavigateToKnockout)
                SectionButton("50 Preguntas", "Verdadero o Falso · 20 pts", Icons.Filled.Quiz, AccentBlue, hasData, onNavigateToQuestions)
                SectionButton("Goleadores", "3 jugadores · 50/30/10 pts", Icons.Filled.Person, AccentOrange, hasData, onNavigateToPlayers)
                SectionButton("Resultados", "Puntos, estadísticas, compartir", Icons.Filled.Update, AccentGreen, hasData, onNavigateToResults)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceMedium.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(46.dp)) {
            Text(match.time, style = MaterialTheme.typography.labelMedium, color = WCGold, fontWeight = FontWeight.Bold)
            Text(match.groupLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Text(match.homeTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.End, maxLines = 1)
        Text(" vs ", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(horizontal = 2.dp))
        Text(match.awayTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1)
        Spacer(modifier = Modifier.width(8.dp))
        Text(match.tvChannel, style = MaterialTheme.typography.labelSmall, color = AccentBlue, modifier = Modifier.width(68.dp), textAlign = TextAlign.Center)
    }
}

// ── Dialog, PointsItem, SectionButton (unchanged) ─────────────

@Composable
private fun ValidationDialog(result: ValidationResult, onDismiss: () -> Unit) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    Dialog(onDismissRequest = { dismissed = true; onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceMedium), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (result.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
                        tint = if (result.isValid) AccentGreen else AccentOrange, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (result.isValid) "EXCEL VÁLIDO" else "EXCEL INCOMPLETO",
                        style = MaterialTheme.typography.titleLarge, color = if (result.isValid) AccentGreen else AccentOrange, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val progress = if (result.totalChecks > 0) result.passedChecks.toFloat() / result.totalChecks else 0f
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(),
                    color = if (result.isValid) AccentGreen else AccentOrange, trackColor = InputBg)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${result.passedChecks}/${result.totalChecks} campos completados", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                CategoryStatus(Icons.Filled.Groups, "Resultados fase de grupos", result.pendingMatches, 72, WCGold)
                CategoryStatus(Icons.Filled.EmojiEvents, "Eliminatorias", result.pendingKnockout, 32, AccentGreen)
                CategoryStatus(Icons.Filled.Quiz, "Preguntas (V/F)", result.pendingQuestions, 50, AccentBlue)
                CategoryStatus(Icons.Filled.Person, "Goleadores", result.pendingPlayers, 3, AccentOrange)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { dismissed = true; onDismiss() }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (result.isValid) AccentGreen else WCBlue), shape = RoundedCornerShape(10.dp)) {
                    Text(if (result.isValid) "CONTINUAR" else "ENTENDIDO", fontWeight = FontWeight.Bold)
                }
                if (!result.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Puedes completar los datos desde la app", style = MaterialTheme.typography.labelSmall, color = TextMuted,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CategoryStatus(icon: ImageVector, label: String, pending: Int, total: Int, color: Color) {
    val completed = total - pending
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Text("$completed/$total", style = MaterialTheme.typography.bodyMedium,
            color = if (pending == 0) AccentGreen else color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PointsItem(label: String, points: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(points.toString(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
private fun SectionButton(title: String, subtitle: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val ca = if (enabled) 1f else 0.4f
    Card(onClick = { if (enabled) onClick() }, Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (enabled) SurfaceMedium else SurfaceMedium.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.2f * ca)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color.copy(alpha = ca), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary.copy(alpha = ca))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = ca))
            }
            if (!enabled) Icon(Icons.Filled.Lock, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            else Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }
    }
}
