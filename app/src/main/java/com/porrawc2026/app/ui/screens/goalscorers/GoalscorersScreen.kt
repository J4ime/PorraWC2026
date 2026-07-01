package com.porrawc2026.app.ui.screens.goalscorers

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.util.PlayerPhotoDownloader
import java.io.File

@Composable
fun GoalscorersScreen(
    viewModel: GoalscorersViewModel = hiltViewModel()
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val topScorers by viewModel.topScorers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (players.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE65100))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE65100)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text("MIS GOLEADORES", fontSize = 12.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.padding(14.dp)) {
                            players.forEach { p -> PlayerRow(p) }
                        }
                    }
                }
            }
        }

        if (topScorers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE65100))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFE65100)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text("PICHICHI", fontSize = 12.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.padding(14.dp)) {
                            topScorers.forEach { scorer -> TopScorerRow(scorer) }
                        }
                    }
                }
            }
        } else if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Cargando goleadores...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlayerRow(p: PlayerPredictionEntity) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPath = p.photoPath?.takeIf { File(it).exists() }
        ?: p.predictedName?.let { name -> PlayerPhotoDownloader.lookupCache(context, name) }
    val photoFile = photoPath?.let { File(it) }
    val hasPoints = p.pointsEarned > 0
    val ptsColor = if (hasPoints) Color(0xFF4CAF50) else Color(0xFF666666)

    Row(Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF333333)), contentAlignment = Alignment.Center) {
            if (photoFile != null) {
                AsyncImage(model = photoFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                val initials = p.predictedName?.split(" ")?.take(2)?.mapNotNull { it.firstOrNull()?.uppercase() }?.joinToString("") ?: "${p.rank}"
                Text(initials, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(p.predictedName ?: "-", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
        Text("${p.pointsEarned} pts", fontSize = 11.sp, color = ptsColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopScorerRow(scorer: TopScorerDisplay) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${scorer.rank} ", fontSize = 10.sp, color = Color(0xFF777777))
        Text(scorer.name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
        if (scorer.flagEmoji.isNotBlank()) { Text(scorer.flagEmoji, fontSize = 14.sp); Spacer(Modifier.width(2.dp)) }
        Spacer(Modifier.weight(1f))
        Text("${scorer.goals} ${if (scorer.goals == 1) "gol" else "goles"}", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
    }
}
