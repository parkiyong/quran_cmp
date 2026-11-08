package com.jetbrains.kmpapp.ui.translations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.TranslationItem
import com.jetbrains.kmpapp.repository.TranslationsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationManagerScreen(
    translationsRepository: TranslationsRepository,
    onNavigateBack: () -> Unit
) {
    val items by translationsRepository.translations.collectAsState()
    val isLoading by translationsRepository.isLoading.collectAsState()
    val errorMessage by translationsRepository.errorMessage.collectAsState()
    val downloadingIds by translationsRepository.downloadingIds.collectAsState()
    val scope = rememberCoroutineScope()

    var filterQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (items.isEmpty()) {
            translationsRepository.refreshTranslationsCatalog()
        } else {
            translationsRepository.syncInstalledStatus()
        }
    }

    val filteredItems = remember(items, filterQuery) {
        if (filterQuery.isBlank()) {
            items
        } else {
            val q = filterQuery.trim().lowercase()
            items.filter {
                it.translation.displayName.lowercase().contains(q) ||
                    (it.translation.translator?.lowercase()?.contains(q) == true) ||
                    (it.translation.translatorForeign?.lowercase()?.contains(q) == true) ||
                    (it.translation.languageCode?.lowercase()?.contains(q) == true)
            }
        }
    }

    val downloadedList = filteredItems.filter { it.isDownloaded }
    val availableList = filteredItems.filter { !it.isDownloaded }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Translations & Tafaseer") },
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
                    IconButton(
                        onClick = {
                            scope.launch { translationsRepository.refreshTranslationsCatalog() }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Catalog",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter by language, title, or translator...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            if (isLoading && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage ?: "Failed to load translations.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (downloadedList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Downloaded (${downloadedList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(downloadedList, key = { "dl_${it.translation.id}" }) { item ->
                            TranslationRowCard(
                                item = item,
                                isDownloading = downloadingIds.containsKey(item.translation.id),
                                onToggleActive = {
                                    translationsRepository.toggleTranslationActive(item.translation)
                                },
                                onMoveUp = {
                                    translationsRepository.moveTranslationOrder(item.translation, -1)
                                },
                                onMoveDown = {
                                    translationsRepository.moveTranslationOrder(item.translation, 1)
                                },
                                onDownload = {},
                                onDelete = {
                                    translationsRepository.deleteTranslation(item.translation)
                                }
                            )
                        }
                    }

                    if (availableList.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Available to Download (${availableList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(availableList, key = { "avail_${it.translation.id}" }) { item ->
                            TranslationRowCard(
                                item = item,
                                isDownloading = downloadingIds.containsKey(item.translation.id),
                                onToggleActive = {},
                                onMoveUp = {},
                                onMoveDown = {},
                                onDownload = {
                                    scope.launch {
                                        translationsRepository.downloadTranslation(item.translation)
                                    }
                                },
                                onDelete = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationRowCard(
    item: TranslationItem,
    isDownloading: Boolean,
    onToggleActive: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val tr = item.translation
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isActive) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isDownloaded) {
                Checkbox(
                    checked = item.isActive,
                    onCheckedChange = { onToggleActive() }
                )
                Spacer(Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val translatorInfo = listOfNotNull(
                    tr.languageCode?.uppercase(),
                    tr.translatorForeign ?: tr.translator
                ).joinToString(" • ")
                if (translatorInfo.isNotBlank()) {
                    Text(
                        text = translatorInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            } else if (item.isDownloaded) {
                if (item.isActive) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Translation")
                }
            } else {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = "Download Translation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
