package com.porrawc2026.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.util.ValidationResult

@Composable
fun HomeScreen(
    onNavigateToMatches: () -> Unit,
    onNavigateToQuestions: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val players by viewModel.players.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importExcel(it) } }

    validationResult?.let { result ->
        ValidationDialog(result = result, onDismiss = { viewModel.dismissValidation() })
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text("Porra Mundial 26", Modifier.align(Alignment.CenterStart), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF333333)).align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                    Text("$totalPoints", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1A1A1A)) {
                Button(
                    onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White), shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(if (hasData) Icons.Filled.Refresh else Icons.Filled.FileUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasData) "Actualizar Excel" else "Cargar Excel", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        },
        containerColor = Color(0xFF0E0E0E)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (upcomingMatches.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(upcomingMatches.firstOrNull()?.dateLabel?.uppercase() ?: "PR\u00D3XIMOS PARTIDOS", style = MaterialTheme.typography.titleSmall,
                                color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            upcomingMatches.take(8).forEach { match ->
                                MatchRow(match)
                                if (match != upcomingMatches.take(8).last()) Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            } else if (hasData) {
                item { Text("Sin partidos próximos", Modifier.fillMaxWidth().padding(24.dp), color = Color(0xFF777777), textAlign = TextAlign.Center) }
            }

            if (!hasData) { item { Spacer(Modifier.height(16.dp)) }; return@LazyColumn }

            if (players.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("GOLEADORES", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            players.forEach { p -> PlayerRow(p) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionButton("Partidos", "Grupos \u00B7 Dieciseisavos \u00B7 Octavos \u00B7 Cuartos \u00B7 Semis \u00B7 Final", Icons.Filled.EmojiEvents, hasData, onNavigateToMatches)
                    SectionButton("50 Preguntas", "Verdadero o Falso \u00B7 20 pts", Icons.Filled.Quiz, hasData, onNavigateToQuestions)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerRow(p: PlayerPredictionEntity) {
    val medals = listOf("\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49")
    Row(Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(medals[p.rank - 1], fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.predictedName ?: "-", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
            Text("${p.pointsPerGoal} pts/gol", style = MaterialTheme.typography.labelSmall, color = Color(0xFF777777))
        }
        Text("${p.goalsScored} goles", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("+${p.pointsEarned}", Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(match.time.ifBlank { "?" }, Modifier.width(38.dp), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Text(match.homeTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.End, maxLines = 1)
        Text(match.homeFlag, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 3.dp))
        val hasScore = match.homeGoals != null && match.awayGoals != null
        Text(if (hasScore) "${match.homeGoals}" else "-", style = MaterialTheme.typography.bodySmall, color = if (hasScore) Color.White else Color(0xFF777777), fontWeight = FontWeight.Bold)
        Text(" - ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF777777))
        Text(if (hasScore) "${match.awayGoals}" else "-", style = MaterialTheme.typography.bodySmall, color = if (hasScore) Color.White else Color(0xFF777777), fontWeight = FontWeight.Bold)
        Text(match.awayFlag, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 3.dp))
        Text(match.awayTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
        channels.forEach { ch ->
            val bg = if (ch.contains("RTVE", ignoreCase = true)) Color(0xFF555555) else Color(0xFF333333)
            Text(ch.trim().take(4), fontSize = 8.sp, color = Color.White, modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
        }
        if (match.status == MatchStatus.LIVE) {
            val infiniteTransition = rememberInfiniteTransition("live_${match.id}")
            val alpha by infiniteTransition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
            Text("LIVE", color = Color(0xFF888888).copy(alpha = alpha), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
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
                Icon(if (result.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
                    tint = if (result.isValid) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (result.isValid) "EXCEL V\u00C1LIDO" else "EXCEL INCOMPLETO",
                    style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                if (!result.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No es v\u00E1lido. Revisa el Excel y vuelve a cargarlo.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF999999), textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { dismissed = true; onDismiss() }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (result.isValid) Color(0xFF444444) else Color(0xFF333333)), shape = RoundedCornerShape(10.dp)) {
                    Text(if (result.isValid) "CONTINUAR" else "ENTENDIDO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionButton(title: String, subtitle: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Card(onClick = { if (enabled) onClick() }, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF999999))
            }
            if (!enabled) Icon(Icons.Filled.Lock, null, tint = Color(0xFF555555), modifier = Modifier.size(18.dp))
            else Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF555555))
        }
    }
}
