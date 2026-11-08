package com.jetbrains.kmpapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.PageAppearance
import com.jetbrains.kmpapp.repository.QuranRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: QuranRepository,
    onOpenMushafSelector: () -> Unit,
    onOpenTranslationsManager: () -> Unit,
    onOpenAudioManager: () -> Unit,
    onOpenDataPrompt: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val settings by repository.settingsRepository.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quran Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Mushaf & Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Active Mushaf Type", fontWeight = FontWeight.SemiBold)
                                Text(
                                    settings.mushafType.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = onOpenMushafSelector) {
                                Text("Change")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onOpenDataPrompt, modifier = Modifier.weight(1f)) {
                                Text("Download Mushaf Data")
                            }
                            OutlinedButton(onClick = onOpenTranslationsManager, modifier = Modifier.weight(1f)) {
                                Text("Translations")
                            }
                        }
                        OutlinedButton(onClick = onOpenAudioManager, modifier = Modifier.fillMaxWidth()) {
                            Text("Manage Offline Audio Recitations")
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Reader Appearance & Layout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Page Theme", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PageAppearance.entries.forEach { app ->
                                FilterChip(
                                    selected = settings.pageAppearance == app,
                                    onClick = { repository.settingsRepository.setPageAppearance(app) },
                                    label = { Text(app.displayName) }
                                )
                            }
                        }

                        if (settings.pageAppearance == PageAppearance.NIGHT) {
                            Text("Night Mode Text Brightness (${settings.nightModeTextBrightness})")
                            Slider(
                                value = settings.nightModeTextBrightness.toFloat(),
                                onValueChange = {
                                    repository.settingsRepository.setNightModeBrightness(
                                        it.toInt(),
                                        settings.nightModeBackgroundBrightness
                                    )
                                },
                                valueRange = 50f..255f
                            )
                            Text("Night Mode Background Brightness (${settings.nightModeBackgroundBrightness})")
                            Slider(
                                value = settings.nightModeBackgroundBrightness.toFloat(),
                                onValueChange = {
                                    repository.settingsRepository.setNightModeBrightness(
                                        settings.nightModeTextBrightness,
                                        it.toInt()
                                    )
                                },
                                valueRange = 0f..150f
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dual-Page Spread Mode", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Display 2 Mushaf pages side-by-side in RTL order on wide screens/landscape",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isDualPageEnabled,
                                onCheckedChange = { repository.settingsRepository.setDualPageEnabled(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Page Header & Footer Info", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Display Surah name, Juz'/Rub' indicator, and page number on Mushaf pages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showPageInfoOverlay,
                                onCheckedChange = { repository.settingsRepository.setShowPageInfoOverlay(it) }
                            )
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Translation Mode Typography",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Arabic Verse Above Translation", fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = settings.showArabicInTranslation,
                                onCheckedChange = { repository.settingsRepository.setShowArabicInTranslation(it) }
                            )
                        }

                        Text("Arabic Font Size (${settings.arabicFontSize.toInt()} sp)")
                        Slider(
                            value = settings.arabicFontSize,
                            onValueChange = {
                                repository.settingsRepository.setFontSizes(it, settings.translationFontSize)
                            },
                            valueRange = 18f..48f
                        )

                        Text("Translation Font Size (${settings.translationFontSize.toInt()} sp)")
                        Slider(
                            value = settings.translationFontSize,
                            onValueChange = {
                                repository.settingsRepository.setFontSizes(settings.arabicFontSize, it)
                            },
                            valueRange = 12f..32f
                        )
                    }
                }
            }
        }
    }
}
