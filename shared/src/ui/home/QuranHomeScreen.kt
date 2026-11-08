package com.jetbrains.kmpapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.core.QuranDisplayData
import com.jetbrains.kmpapp.data.core.QuranInfo
import com.jetbrains.kmpapp.data.model.Bookmark
import com.jetbrains.kmpapp.data.model.RecentPage
import com.jetbrains.kmpapp.data.model.SuraAyah
import com.jetbrains.kmpapp.data.model.Tag
import com.jetbrains.kmpapp.repository.QuranRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHomeScreen(
    repository: QuranRepository,
    onOpenPage: (page: Int, highlightVerse: SuraAyah?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMushafSelector: () -> Unit,
    onOpenTranslationsManager: () -> Unit,
    onOpenAudioManager: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by repository.settingsRepository.settings.collectAsState()
    val bookmarks by repository.bookmarksHandler.bookmarks.collectAsState()
    val tags by repository.bookmarksHandler.tags.collectAsState()
    val recentPages by repository.bookmarksHandler.recentPages.collectAsState()

    val quranInfo = remember(settings.mushafType) {
        repository.getQuranInfo(settings.mushafType)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Quran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search Quran")
                        }
                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(Icons.Default.FastForward, contentDescription = "Jump to Page / Ayah")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Switch Mushaf") },
                                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                                    onClick = { showOverflowMenu = false; onOpenMushafSelector() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Translations") },
                                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null) },
                                    onClick = { showOverflowMenu = false; onOpenTranslationsManager() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Audio Manager") },
                                    leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                                    onClick = { showOverflowMenu = false; onOpenAudioManager() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = { showOverflowMenu = false; onOpenSettings() }
                                )
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("SURAHS") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("JUZ'") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("BOOKMARKS") }
                    )
                }
            }
        },
        floatingActionButton = {
            val lastPage = repository.settingsRepository.getLastPage(settings.mushafType)
            FloatingActionButton(
                onClick = { onOpenPage(lastPage, null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continue (p. $lastPage)")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SurahsTab(
                    quranInfo = quranInfo,
                    recentPages = recentPages,
                    showTranslatedName = settings.showSuraTranslatedName,
                    onOpenPage = onOpenPage
                )
                1 -> JuzQuartersTab(
                    quranInfo = quranInfo,
                    onOpenPage = onOpenPage
                )
                2 -> BookmarksAndTagsTab(
                    repository = repository,
                    quranInfo = quranInfo,
                    bookmarks = bookmarks,
                    tags = tags,
                    recentPages = recentPages,
                    onOpenPage = onOpenPage
                )
            }
        }
    }

    if (showJumpDialog) {
        JumpToPageOrVerseDialog(
            quranInfo = quranInfo,
            onDismiss = { showJumpDialog = false },
            onJump = { page, verse ->
                showJumpDialog = false
                onOpenPage(page, verse)
            }
        )
    }
}

@Composable
private fun SurahsTab(
    quranInfo: QuranInfo,
    recentPages: List<RecentPage>,
    showTranslatedName: Boolean,
    onOpenPage: (Int, SuraAyah?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        if (recentPages.isNotEmpty()) {
            item {
                val recent = recentPages.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { onOpenPage(recent.page, null) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recent Page: ${QuranDisplayData.getSuraNameFromPage(quranInfo, recent.page)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Page ${recent.page} • Juz' ${quranInfo.getJuzFromPage(recent.page)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "p. ${recent.page}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        items((1..114).toList(), key = { it }) { sura ->
            val page = quranInfo.getPageNumberForSura(sura)
            val ayahs = quranInfo.getNumberOfAyahs(sura)
            val isMakki = quranInfo.isMakki(sura)
            val suraName = QuranDisplayData.getSuraName(sura, wantPrefix = true)
            val translatedName = QuranDisplayData.getSuraTranslation(sura)
            val arabicName = QuranDisplayData.getSuraArabicName(sura)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPage(page, null) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sura.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suraName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val meta = buildString {
                        if (showTranslatedName) {
                            append(translatedName)
                            append(" • ")
                        }
                        append(if (isMakki) "Makkiyah" else "Madaniyah")
                        append(" • $ayahs Ayahs")
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "سورة $arabicName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Page $page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 70.dp))
        }
    }
}

@Composable
private fun JuzQuartersTab(
    quranInfo: QuranInfo,
    onOpenPage: (Int, SuraAyah?) -> Unit
) {
    val quarters = quranInfo.quarters
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        itemsIndexed(quarters) { index, verse ->
            val juzNumber = (index / 8) + 1
            val hizbNumber = (index / 4) + 1
            val quarterMod = index % 4
            val isStartOfJuz = (index % 8 == 0)
            val page = quranInfo.getPageFromSuraAyah(verse.sura, verse.ayah)
            val quarterPreview = QuranDisplayData.quarterPrefixArray.getOrNull(index) ?: ""
            val suraName = QuranDisplayData.getSuraName(verse.sura, wantPrefix = true)

            if (isStartOfJuz) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(quranInfo.getStartingPageForJuz(juzNumber), null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Juz' $juzNumber (${QuranDisplayData.toArabicNumber(juzNumber)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Page ${quranInfo.getStartingPageForJuz(juzNumber)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val rubLabel = when (quarterMod) {
                0 -> "Hizb $hizbNumber"
                1 -> "1/4 Hizb $hizbNumber"
                2 -> "1/2 Hizb $hizbNumber"
                else -> "3/4 Hizb $hizbNumber"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPage(page, verse) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val symbol = when (quarterMod) {
                        0 -> "۞"
                        1 -> "¼"
                        2 -> "½"
                        else -> "¾"
                    }
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quarterPreview,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$rubLabel • $suraName ${verse.sura}:${verse.ayah}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "p. $page",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookmarksAndTagsTab(
    repository: QuranRepository,
    quranInfo: QuranInfo,
    bookmarks: List<Bookmark>,
    tags: List<Tag>,
    recentPages: List<RecentPage>,
    onOpenPage: (Int, SuraAyah?) -> Unit
) {
    var selectedTagId by remember { mutableStateOf<Long?>(null) }
    var showNewTagDialog by remember { mutableStateOf(false) }
    var bookmarkToTag by remember { mutableStateOf<Bookmark?>(null) }

    val tagMap = remember(tags) { tags.associateBy { it.id } }

    val filteredBookmarks = remember(bookmarks, selectedTagId) {
        if (selectedTagId == null) {
            bookmarks
        } else {
            bookmarks.filter { it.tags.contains(selectedTagId) }
        }
    }

    val pageBookmarks = filteredBookmarks.filter { it.isPageBookmark }
    val ayahBookmarks = filteredBookmarks.filter { !it.isPageBookmark }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Tag",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = { showNewTagDialog = true },
                    label = { Text("New Tag") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedTagId == null,
                    onClick = { selectedTagId = null },
                    label = { Text("All (${bookmarks.size})") }
                )
                tags.forEach { tag ->
                    FilterChip(
                        selected = selectedTagId == tag.id,
                        onClick = {
                            selectedTagId = if (selectedTagId == tag.id) null else tag.id
                        },
                        label = { Text(tag.name) }
                    )
                }
            }
        }

        if (recentPages.isNotEmpty() && selectedTagId == null) {
            item {
                Text(
                    text = "Recent Pages",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(recentPages, key = { "recent_${it.page}" }) { recent ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPage(recent.page, null) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = QuranDisplayData.getSuraNameFromPage(quranInfo, recent.page),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Page ${recent.page} • Juz' ${quranInfo.getJuzFromPage(recent.page)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (pageBookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "Page Bookmarks (${pageBookmarks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(pageBookmarks, key = { "bm_${it.id}" }) { bm ->
                BookmarkRowCard(
                    bookmark = bm,
                    tagMap = tagMap,
                    title = QuranDisplayData.getSuraNameFromPage(quranInfo, bm.page),
                    subtitle = "Page ${bm.page} • Juz' ${quranInfo.getJuzFromPage(bm.page)}",
                    onClick = { onOpenPage(bm.page, null) },
                    onTagClick = { bookmarkToTag = bm },
                    onDeleteClick = { repository.bookmarksHandler.deleteBookmark(bm.id) }
                )
            }
        }

        if (ayahBookmarks.isNotEmpty()) {
            item {
                Text(
                    text = "Ayah Bookmarks (${ayahBookmarks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(ayahBookmarks, key = { "bm_ayah_${it.id}" }) { bm ->
                val sura = bm.sura ?: 1
                val ayah = bm.ayah ?: 1
                BookmarkRowCard(
                    bookmark = bm,
                    tagMap = tagMap,
                    title = "${QuranDisplayData.getSuraName(sura, true)} ($sura:$ayah)",
                    subtitle = "Page ${bm.page} • Juz' ${quranInfo.getJuzFromPage(bm.page)}",
                    onClick = { onOpenPage(bm.page, SuraAyah(sura, ayah)) },
                    onTagClick = { bookmarkToTag = bm },
                    onDeleteClick = { repository.bookmarksHandler.deleteBookmark(bm.id) }
                )
            }
        }

        if (pageBookmarks.isEmpty() && ayahBookmarks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bookmarks yet. Tap the bookmark icon in the reader or long-press any Ayah to bookmark and tag it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showNewTagDialog) {
        var tagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("Create New Tag") },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            repository.bookmarksHandler.addTag(tagName)
                            showNewTagDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    bookmarkToTag?.let { bm ->
        var selectedIds by remember(bm) {
            mutableStateOf(bm.tags.toSet())
        }
        AlertDialog(
            onDismissRequest = { bookmarkToTag = null },
            title = { Text("Assign Tags") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tags.isEmpty()) {
                        Text("No tags yet. Create a tag first.")
                    } else {
                        tags.forEach { tag ->
                            val checked = selectedIds.contains(tag.id)
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    selectedIds = if (checked) selectedIds - tag.id else selectedIds + tag.id
                                },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.bookmarksHandler.setBookmarkTags(bm.id, selectedIds)
                        bookmarkToTag = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToTag = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookmarkRowCard(
    bookmark: Bookmark,
    tagMap: Map<Long, Tag>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onTagClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onTagClick) {
                    Icon(Icons.Default.Label, contentDescription = "Tag Bookmark")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark")
                }
            }
            if (bookmark.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 36.dp, top = 4.dp)
                ) {
                    bookmark.tags.forEach { tagId ->
                        val tagName = tagMap[tagId]?.name ?: "#$tagId"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tagName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JumpToPageOrVerseDialog(
    quranInfo: QuranInfo,
    onDismiss: () -> Unit,
    onJump: (page: Int, verse: SuraAyah?) -> Unit
) {
    var pageInput by remember { mutableStateOf("") }
    var suraInput by remember { mutableStateOf("1") }
    var ayahInput by remember { mutableStateOf("1") }
    var juzInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jump to Page, Surah, or Juz'") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Page (1 - ${quranInfo.numberOfPages})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = juzInput,
                    onValueChange = { juzInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Or Juz' (1 - 30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = suraInput,
                        onValueChange = { suraInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Surah (1-114)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ayahInput,
                        onValueChange = { ayahInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Ayah") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = pageInput.toIntOrNull()
                    val j = juzInput.toIntOrNull()
                    if (p != null && quranInfo.isValidPage(p)) {
                        onJump(p, null)
                    } else if (j != null && j in 1..30) {
                        val jPage = quranInfo.getStartingPageForJuz(j)
                        onJump(jPage, null)
                    } else {
                        val s = (suraInput.toIntOrNull() ?: 1).coerceIn(1, 114)
                        val maxAyah = quranInfo.getNumberOfAyahs(s).coerceAtLeast(1)
                        val a = (ayahInput.toIntOrNull() ?: 1).coerceIn(1, maxAyah)
                        val targetPage = quranInfo.getPageFromSuraAyah(s, a)
                        if (quranInfo.isValidPage(targetPage)) {
                            onJump(targetPage, SuraAyah(s, a))
                        }
                    }
                }
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
