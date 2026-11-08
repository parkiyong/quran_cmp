package com.jetbrains.kmpapp.ui.dataprompt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.MushafType
import com.jetbrains.kmpapp.io.DownloadStatus
import com.jetbrains.kmpapp.repository.QuranRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranDataScreen(
    repository: QuranRepository,
    mushafType: MushafType,
    onBundleReady: () -> Unit,
    onOpenMushafSelector: () -> Unit
) {
    val downloadProgress by repository.downloadManager.progress.collectAsState()
    val scope = rememberCoroutineScope()
    val provider = repository.getPageProvider(mushafType)

    LaunchedEffect(mushafType, downloadProgress.status) {
        if (repository.isMushafBundleReady(mushafType)) {
            onBundleReady()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quran Data Setup") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    OutlinedButton(
                        onClick = onOpenMushafSelector,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            mushafType.displayName,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = provider.getPreviewTitle(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = provider.getPreviewDescription(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "To render high-resolution Mushaf pages (${provider.dataSource.numberOfPages} pages), " +
                            "ayah coordinate bounding boxes, and Uthmani/Warsh Arabic text offline, " +
                            "Quran needs to download the required data bundle from android.quran.com.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    when (downloadProgress.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.EXTRACTING -> {
                            Text(
                                text = downloadProgress.currentTaskTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            if (downloadProgress.totalBytes > 0) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress.fraction },
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                val dlMb = (downloadProgress.downloadedBytes / (1024f * 1024f))
                                val totalMb = (downloadProgress.totalBytes / (1024f * 1024f))
                                Text(
                                    text = "${formatMb(dlMb)} / ${formatMb(totalMb)} MB (${(downloadProgress.fraction * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                val dlMb = (downloadProgress.downloadedBytes / (1024f * 1024f))
                                Text(
                                    text = if (downloadProgress.status == DownloadStatus.EXTRACTING) {
                                        "Extracting archive..."
                                    } else {
                                        "Downloaded ${formatMb(dlMb)} MB..."
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        DownloadStatus.ERROR -> {
                            Text(
                                text = "Download failed: ${downloadProgress.errorMessage ?: "Network error"}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = repository.downloadManager.downloadAndInstallMushafBundle(provider)
                                        if (ok) onBundleReady()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry Download")
                            }
                        }

                        else -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = repository.downloadManager.downloadAndInstallMushafBundle(provider)
                                        if (ok) onBundleReady()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download ${provider.getPreviewTitle()} Files")
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenMushafSelector,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Switch Mushaf Type")
                                }
                                OutlinedButton(
                                    onClick = onBundleReady,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Browse Index First")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMb(value: Float): String {
    val rounded = (value * 10).toInt() / 10.0
    return rounded.toString()
}
