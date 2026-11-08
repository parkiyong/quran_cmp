package com.jetbrains.kmpapp.ui.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.core.QuranDisplayData
import com.jetbrains.kmpapp.data.model.Qari
import com.jetbrains.kmpapp.repository.AudioPlaybackController
import kotlinx.coroutines.launch

@Composable
fun AudioBar(
    audioController: AudioPlaybackController,
    currentPage: Int,
    onOpenQariSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by audioController.state.collectAsState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenQariSelector)
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = "Select Qari",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = state.selectedQari.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = state.statusMessage
                            ?: state.currentVerse?.let {
                                "${QuranDisplayData.getSuraName(it.sura, true)} (${it.sura}:${it.ayah})"
                            }
                            ?: if (state.selectedQari.isGapless) "Gapless Recitation" else "Verse-by-Verse"
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Speed & Repeat chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { audioController.cyclePlaybackSpeed() },
                        label = { Text("${state.playbackSpeed}x") },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                    val repeatLabel = when (state.verseRepeatCount) {
                        0 -> "1x"
                        -1 -> "∞"
                        else -> "${state.verseRepeatCount + 1}x"
                    }
                    AssistChip(
                        onClick = { audioController.cycleVerseRepeat() },
                        label = { Text(repeatLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.RepeatOne, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { audioController.previousAyah() },
                    enabled = state.currentVerse != null
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Ayah")
                }

                IconButton(
                    onClick = {
                        if (state.isPlaying || state.isPaused) {
                            audioController.togglePlayPause()
                        } else {
                            audioController.playPage(currentPage)
                        }
                    }
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { audioController.nextAyah() },
                    enabled = state.currentVerse != null
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next Ayah")
                }

                IconButton(
                    onClick = { audioController.stop() },
                    enabled = state.isPlaying || state.isPaused || state.currentVerse != null
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QariListSheet(
    audioController: AudioPlaybackController,
    onDismiss: () -> Unit
) {
    val state by audioController.state.collectAsState()
    val qaris = audioController.getAvailableQaris()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = "Select Reciter (Qari)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn {
                items(qaris, key = { it.id }) { qari ->
                    val isSelected = qari.id == state.selectedQari.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audioController.selectQari(qari)
                                onDismiss()
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                audioController.selectQari(qari)
                                onDismiss()
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = qari.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (qari.isGapless) "Gapless (Continuous Surah MP3)" else "Verse-by-Verse (Gapped)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagerScreen(
    audioController: AudioPlaybackController,
    onNavigateBack: () -> Unit
) {
    val state by audioController.state.collectAsState()
    var showQariSheet by remember { mutableStateOf(false) }
    val downloadingProgress = remember { mutableStateMapOf<Int, Float>() }
    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Audio Manager")
                        Text(
                            text = state.selectedQari.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showQariSheet = true }) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = "Switch Qari",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..114).toList(), key = { "${state.selectedQari.id}_${it}_$refreshTrigger" }) { sura ->
                val isDownloaded = audioController.isSuraDownloaded(state.selectedQari, sura)
                val progress = downloadingProgress[sura]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$sura. ${QuranDisplayData.getSuraName(sura, true)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "سورة ${QuranDisplayData.getSuraArabicName(sura)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (progress != null) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                            } else if (isDownloaded) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            audioController.deleteSuraAudio(state.selectedQari, sura)
                                            refreshTrigger++
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Audio")
                                    }
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            downloadingProgress[sura] = 0.05f
                                            audioController.downloadSuraAudio(
                                                qari = state.selectedQari,
                                                sura = sura,
                                                onProgress = { p -> downloadingProgress[sura] = p }
                                            )
                                            downloadingProgress.remove(sura)
                                            refreshTrigger++
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Download Surah Audio")
                                }
                            }
                        }

                        if (progress != null) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQariSheet) {
        QariListSheet(
            audioController = audioController,
            onDismiss = { showQariSheet = false }
        )
    }
}
