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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.porrawc2026.app.R
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.util.PlayerPhotoDownloader
import com.porrawc2026.app.util.ValidationResult
import java.io.File

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

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Borrar datos", color = Color.White) },
            text = { Text("Se eliminarán todos los datos importados del Excel. ¿Estás seguro?", color = Color(0xFF999999)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))) {
                    Text("BORRAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF777777))) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Image(painter = painterResource(R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(36.dp).align(Alignment.CenterStart))
                Text("PORRA MUNDIAL 26", Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF333333)).align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                    Text("$totalPoints", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = Color(0xFF1A1A1A)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color.White), shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(if (hasData) Icons.Filled.Refresh else Icons.Filled.FileUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (hasData) "Actualizar" else "Cargar Excel", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    if (hasData) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444), contentColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0E0E0E)
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (upcomingMatches.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(upcomingMatches.firstOrNull()?.dateLabel?.uppercase() ?: "PR\u00D3XIMOS PARTIDOS", style = MaterialTheme.typography.titleSmall,
                            color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        upcomingMatches.take(8).forEach { match ->
                            MatchRow(match)
                            if (match != upcomingMatches.take(8).last()) Spacer(Modifier.height(4.dp))
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
                    SectionButton("Partidos", "Resultados de la fase de grupos y eliminatorias", Icons.Filled.EmojiEvents, hasData, onNavigateToMatches)
                    SectionButton("50 Preguntas", "Verdadero o Falso \u00B7 20 pts", Icons.Filled.Quiz, hasData, onNavigateToQuestions)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerRow(p: PlayerPredictionEntity) {
    val context = LocalContext.current
    val photoPath = p.photoPath?.takeIf { File(it).exists() }
        ?: p.predictedName?.let { name -> PlayerPhotoDownloader.lookupCache(context, name) }
    val photoFile = photoPath?.let { File(it) }

    Row(Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            if (photoFile != null) {
                AsyncImage(model = photoFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                val initials = p.predictedName?.split(" ")?.take(2)?.mapNotNull { it.firstOrNull()?.uppercase() }?.joinToString("") ?: "${p.rank}"
                Text(initials, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.predictedName ?: "-", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
            Text("${p.pointsPerGoal} pts/gol", style = MaterialTheme.typography.labelSmall, color = Color(0xFF777777))
        }
        Text("${p.goalsScored} goles", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("${p.pointsEarned}", Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val isLive = match.status == MatchStatus.LIVE

    val scoreBg = when {
        isLive -> Color(0xFF2E7D32)
        hasResult -> Color.Transparent
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(match.time.ifBlank { "?" }, Modifier.width(36.dp),
            style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold,
            maxLines = 1, softWrap = false)
        Text(match.homeTeam, Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall, color = Color.White,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)

        Box(
            modifier = Modifier.padding(horizontal = 4.dp).background(scoreBg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (hasResult || isLive) {
                val h = if (hasResult) "${match.homeGoals}" else (match.homeGoals?.toString() ?: "0")
                val a = if (hasResult) "${match.awayGoals}" else (match.awayGoals?.toString() ?: "0")
                Text("$h - $a",
                    style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold,
                    maxLines = 1)
            } else if (hasPred) {
                Text("-",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777),
                    maxLines = 1)
            } else {
                Text("-",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777),
                    maxLines = 1)
            }
        }

        Text(match.awayTeam, Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis)

        Spacer(Modifier.width(4.dp))
        val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
        channels.forEach { ch ->
            val bg = if (ch.contains("RTVE", ignoreCase = true)) Color(0xFF0037A1) else Color(0xFF333333)
            Text(ch.trim().take(4), fontSize = 8.sp, color = Color.White,
                modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
        }

        if (isLive) {
            val infiniteTransition = rememberInfiniteTransition("live_${match.id}")
            val alpha by infiniteTransition.animateFloat(1f, 0.3f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
            Text("LIVE", color = Color(0xFF888888).copy(alpha = alpha),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 2.dp))
        }

        if (hasResult || (hasPred && isLive)) {
            val ptsValue = if (match.pointsEarned > 0) "${match.pointsEarned}" else "0"
            val ptsBg = if (isLive) Color(0xFF2E7D32) else Color.Transparent
            val ptsColor = when {
                isLive -> Color.White
                hasResult && match.pointsEarned > 0 -> Color(0xFF4CAF50)
                else -> Color(0xFF666666)
            }
            Text(ptsValue,
                modifier = Modifier.width(24.dp).background(ptsBg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = ptsColor,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
