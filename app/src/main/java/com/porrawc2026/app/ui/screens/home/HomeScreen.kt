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
    val sectionTitle by viewModel.sectionTitle.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importExcel(it) }
    }

    validationResult?.let { result ->
        ValidationDialog(result = result, onDismiss = { viewModel.dismissValidation() })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(WCDarkBlue, WCNavy, SurfaceDark)))
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(WCBlue, WCDarkBlue, WCBlue)))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "\uD83C\uDFC6", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PORRA", style = MaterialTheme.typography.displayLarge, color = WCGold, fontWeight = FontWeight.Black)
                    Text("MUNDIAL 2026", style = MaterialTheme.typography.displayMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("USA \u00B7 M\u00C9XICO \u00B7 CANAD\u00C1", style = MaterialTheme.typography.titleMedium, color = WCGoldLight, letterSpacing = 3.sp)
                }
            }
        }

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
                    Icon(if (hasData) Icons.Filled.Refresh else Icons.Filled.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasData) "Actualizar Excel" else "Cargar Excel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!hasData) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            return@LazyColumn
        }

        // Upcoming matches
        if (upcomingMatches.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            upcomingMatches.firstOrNull()?.dateLabel?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            color = WCGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        upcomingMatches.take(8).forEach { match ->
                            MatchRow(match)
                            if (match != upcomingMatches.take(8).last()) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
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

        item {
            Text("SECCIONES", style = MaterialTheme.typography.labelMedium, color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), letterSpacing = 2.sp)
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionButton("Fase de Grupos", "Predicciones de 72 partidos", Icons.Filled.Groups, WCGold, hasData, onNavigateToGroups)
                SectionButton("Eliminatorias", "Dieciseisavos a la Final", Icons.Filled.EmojiEvents, WCGold, hasData, onNavigateToKnockout)
                SectionButton("50 Preguntas", "Verdadero o Falso \u00B7 20 pts", Icons.Filled.Quiz, AccentBlue, hasData, onNavigateToQuestions)
                SectionButton("Goleadores", "3 jugadores \u00B7 50/30/10 pts", Icons.Filled.Person, AccentOrange, hasData, onNavigateToPlayers)
                SectionButton("Resultados", "Puntos, estad\u00EDsticas, compartir", Icons.Filled.Update, AccentGreen, hasData, onNavigateToResults)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    val hasScore = match.homeGoals != null && match.awayGoals != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when (match.status) {
                    MatchStatus.LIVE -> AccentRed.copy(alpha = 0.1f)
                    MatchStatus.FINISHED -> SurfaceMedium.copy(alpha = 0.2f)
                    MatchStatus.UPCOMING -> SurfaceMedium.copy(alpha = 0.3f)
                },
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            match.time.ifBlank { "?" },
            style = MaterialTheme.typography.labelMedium,
            color = WCGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(38.dp)
        )

        Text(
            match.homeTeam,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1
        )

        Text(
            match.homeFlag,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 3.dp)
        )

        Text(
            if (hasScore) "${match.homeGoals}" else "-",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasScore) TextPrimary else TextMuted,
            fontWeight = FontWeight.Bold
        )

        Text(
            " - ",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        Text(
            if (hasScore) "${match.awayGoals}" else "-",
            style = MaterialTheme.typography.bodySmall,
            color = if (hasScore) TextPrimary else TextMuted,
            fontWeight = FontWeight.Bold
        )

        Text(
            match.awayFlag,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 3.dp)
        )

        Text(
            match.awayTeam,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(4.dp))

        TvChannelBadge(match.tvChannel)

        if (match.status == MatchStatus.LIVE) {
            val infiniteTransition = rememberInfiniteTransition("live_${match.id}")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse)
            )
            Text("LIVE", color = AccentRed.copy(alpha = alpha), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

// ── Validation dialog ─────────────

@Composable
private fun ValidationDialog(result: ValidationResult, onDismiss: () -> Unit) {
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    Dialog(onDismissRequest = { dismissed = true; onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceMedium), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (result.isValid) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
                    tint = if (result.isValid) AccentGreen else AccentOrange, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (result.isValid) "EXCEL V\u00C1LIDO" else "EXCEL INCOMPLETO",
                    style = MaterialTheme.typography.titleLarge, color = if (result.isValid) AccentGreen else AccentOrange, fontWeight = FontWeight.Bold)
                if (!result.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No es v\u00E1lido. Revisa el Excel y vuelve a cargarlo.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { dismissed = true; onDismiss() }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (result.isValid) AccentGreen else WCBlue), shape = RoundedCornerShape(10.dp)) {
                    Text(if (result.isValid) "CONTINUAR" else "ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            }
        }
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
private fun TvChannelBadge(channel: String) {
    if (channel.isBlank() || channel.all { it.isDigit() || it == '.' }) return
    val (bg, fg) = when (channel.uppercase()) {
        "RTVE", "TVE", "LA 1", "LA1", "LA 2", "LA2", "TELEDEPORTE", "RTVE PLAY" -> Color(0xFF0B1D3A) to Color.White
        "DAZN", "DAZN MUNDIAL", "DAZN APP GRATIS" -> Color(0xFF0A0A0A) to Color.White
        "MOVISTAR", "M+", "MOVISTAR+", "#VAMOS" -> Color(0xFF019DF4) to Color.White
        else -> SurfaceMedium to TextPrimary
    }
    val label = when (channel.uppercase()) {
        "RTVE", "TVE", "LA 1", "LA1" -> "RTVE"
        "LA 2", "LA2" -> "TVE2"
        "TELEDEPORTE" -> "TDP"
        "DAZN" -> "DAZN"
        "MOVISTAR", "M+", "MOVISTAR+" -> "M+"
        "#VAMOS" -> "VAMOS"
        else -> channel.take(4).uppercase()
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
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
