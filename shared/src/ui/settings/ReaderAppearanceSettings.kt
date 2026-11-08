package com.jetbrains.kmpapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.PageAppearance
import com.jetbrains.kmpapp.repository.QuranRepository

/**
 * Shared reader appearance & layout controls (page theme, night brightness,
 * dual-page mode, page info overlay). Used by both the Settings screen and the
 * reader's theme bottom sheet so the two never drift apart.
 */
@Composable
fun ReaderAppearanceSettings(
    repository: QuranRepository,
    modifier: Modifier = Modifier
) {
    val settings by repository.settingsRepository.settings.collectAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Page Theme", style = MaterialTheme.typography.labelLarge)

        // Equal-width, 2-column grid so long theme names never get squeezed
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PageAppearance.entries.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { app ->
                        FilterChip(
                            selected = settings.pageAppearance == app,
                            onClick = { repository.settingsRepository.setPageAppearance(app) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = app.displayName,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                    if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                }
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

        SettingSwitchRow(
            title = "Dual-Page Spread Mode",
            description = "Show two pages side by side on wide screens",
            checked = settings.isDualPageEnabled,
            onCheckedChange = { repository.settingsRepository.setDualPageEnabled(it) }
        )

        SettingSwitchRow(
            title = "Show Page Header & Footer Info",
            description = "Surah, Juz' and page number on each page",
            checked = settings.showPageInfoOverlay,
            onCheckedChange = { repository.settingsRepository.setShowPageInfoOverlay(it) }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
