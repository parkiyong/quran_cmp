package com.jetbrains.kmpapp.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.core.QuranDisplayData
import com.jetbrains.kmpapp.data.core.QuranInfo
import com.jetbrains.kmpapp.data.model.AyahCoordinates
import com.jetbrains.kmpapp.data.model.Bookmark
import com.jetbrains.kmpapp.data.model.PageAppearance
import com.jetbrains.kmpapp.data.model.ReaderViewMode
import com.jetbrains.kmpapp.data.model.SuraAyah
import com.jetbrains.kmpapp.platform.copyTextToClipboard
import com.jetbrains.kmpapp.platform.decodeImageBitmapFromFile
import com.jetbrains.kmpapp.platform.encodeImageBitmapToPngBytes
import com.jetbrains.kmpapp.platform.shareOrSavePngImage
import com.jetbrains.kmpapp.platform.shareText
import com.jetbrains.kmpapp.repository.AudioPlaybackController
import com.jetbrains.kmpapp.repository.QuranRepository
import com.jetbrains.kmpapp.ui.settings.ReaderAppearanceSettings
import com.jetbrains.kmpapp.repository.VerseWithTranslations
import com.jetbrains.kmpapp.ui.audio.AudioBar
import com.jetbrains.kmpapp.ui.audio.QariListSheet
import com.jetbrains.kmpapp.ui.theme.AyahAudioHighlight
import com.jetbrains.kmpapp.ui.theme.AyahBookmarkHighlight
import com.jetbrains.kmpapp.ui.theme.AyahSelectionHighlight
import com.jetbrains.kmpapp.ui.theme.PageThemeColors
import com.jetbrains.kmpapp.ui.theme.QuranGoldAccent
import com.jetbrains.kmpapp.ui.theme.QuranTealDark
import com.jetbrains.kmpapp.ui.theme.buildMushafImageColorFilter
import com.jetbrains.kmpapp.ui.theme.resolvePageThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerScreen(
    repository: QuranRepository,
    audioController: AudioPlaybackController,
    initialPage: Int,
    initialHighlightVerse: SuraAyah? = null,
    onOpenTranslationsManager: () -> Unit,
    onOpenDataPrompt: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val settings by repository.settingsRepository.settings.collectAsState()
    val bookmarks by repository.bookmarksHandler.bookmarks.collectAsState()
    val tags by repository.bookmarksHandler.tags.collectAsState()
    val audioState by audioController.state.collectAsState()

    val quranInfo = remember(settings.mushafType) {
        repository.getQuranInfo(settings.mushafType)
    }
    val totalPages = quranInfo.numberOfPages

    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )
    val currentPage = pagerState.currentPage + 1

    var showChrome by remember { mutableStateOf(true) }
    var selectedVerse by remember { mutableStateOf(initialHighlightVerse) }
    var showQuickTafsirSheet by remember { mutableStateOf(false) }
    var showPageThemeSheet by remember { mutableStateOf(false) }
    var showQariSheet by remember { mutableStateOf(false) }
    var showTagDialogForVerse by remember { mutableStateOf<SuraAyah?>(null) }
    var verseForImageShare by remember { mutableStateOf<SuraAyah?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val themeColors = remember(
        settings.pageAppearance,
        settings.nightModeTextBrightness,
        settings.nightModeBackgroundBrightness
    ) {
        resolvePageThemeColors(
            settings.pageAppearance,
            settings.nightModeTextBrightness,
            settings.nightModeBackgroundBrightness
        )
    }

    // Record page navigation in recent pages & settings
    LaunchedEffect(currentPage, settings.mushafType) {
        repository.settingsRepository.setLastPage(settings.mushafType, currentPage)
        repository.bookmarksHandler.recordPageNavigation(currentPage)
    }

    // Listen to AudioPlaybackController active verse updates to auto-advance page
    DisposableEffect(audioController, quranInfo) {
        audioController.onActiveVerseChanged = { _, targetPage ->
            if (quranInfo.isValidPage(targetPage) && targetPage != pagerState.currentPage + 1) {
                scope.launch {
                    pagerState.animateScrollToPage(targetPage - 1)
                }
            }
        }
        onDispose {
            audioController.onActiveVerseChanged = null
        }
    }

    val isCurrentPageBookmarked = remember(bookmarks, currentPage) {
        bookmarks.any { it.isPageBookmark && it.page == currentPage }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.backgroundColor,
        topBar = {
            AnimatedVisibility(visible = showChrome, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = QuranDisplayData.getSuraNameFromPage(quranInfo, currentPage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = QuranDisplayData.getPageSubtitle(quranInfo, currentPage),
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
                        // Toggle Page Bookmark
                        IconButton(
                            onClick = {
                                repository.bookmarksHandler.togglePageBookmark(currentPage)
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark Page",
                                tint = if (isCurrentPageBookmarked) QuranGoldAccent else MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Switch Reader View Mode (Mushaf / Translation / Split)
                        IconButton(
                            onClick = {
                                val nextMode = when (settings.readerViewMode) {
                                    ReaderViewMode.MUSHAF_PAGE -> ReaderViewMode.TRANSLATION
                                    ReaderViewMode.TRANSLATION -> ReaderViewMode.SPLIT_MUSHAF_AND_TRANSLATION
                                    ReaderViewMode.SPLIT_MUSHAF_AND_TRANSLATION -> ReaderViewMode.MUSHAF_PAGE
                                }
                                repository.settingsRepository.setReaderViewMode(nextMode)
                            }
                        ) {
                            val icon = when (settings.readerViewMode) {
                                ReaderViewMode.MUSHAF_PAGE -> Icons.Default.Translate
                                ReaderViewMode.TRANSLATION -> Icons.Default.VerticalSplit
                                ReaderViewMode.SPLIT_MUSHAF_AND_TRANSLATION -> Icons.Default.MenuBook
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Switch View Mode",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Toggle Dual Page Spread
                        IconButton(
                            onClick = {
                                repository.settingsRepository.setDualPageEnabled(!settings.isDualPageEnabled)
                            }
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = "Toggle Dual Page Spread",
                                tint = if (settings.isDualPageEnabled) QuranGoldAccent else MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Page Theme Picker
                        IconButton(onClick = { showPageThemeSheet = true }) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Page Theme & Brightness",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            Column {
                // Selected Ayah Floating Action Bar
                AnimatedVisibility(visible = selectedVerse != null) {
                    selectedVerse?.let { verse ->
                        val isAyahBookmarked = bookmarks.any {
                            it.sura == verse.sura && it.ayah == verse.ayah
                        }
                        AyahSelectionActionBar(
                            verse = verse,
                            isBookmarked = isAyahBookmarked,
                            onBookmarkToggle = {
                                val pageForAyah = quranInfo.getPageFromSuraAyah(verse.sura, verse.ayah)
                                repository.bookmarksHandler.toggleAyahBookmark(
                                    verse.sura,
                                    verse.ayah,
                                    pageForAyah
                                )
                            },
                            onTagClick = {
                                showTagDialogForVerse = verse
                            },
                            onPlayFromHere = {
                                audioController.playFrom(verse)
                            },
                            onQuickTafsir = {
                                showQuickTafsirSheet = true
                            },
                            onCopyText = {
                                scope.launch {
                                    val fullVerse = repository.getSingleVerseWithTranslations(verse.sura, verse.ayah)
                                    val text = repository.formatShareableVerseText(fullVerse)
                                    copyTextToClipboard(text)
                                    snackbarHostState.showSnackbar("Copied ${verse.sura}:${verse.ayah} to clipboard")
                                }
                            },
                            onShareText = {
                                scope.launch {
                                    val fullVerse = repository.getSingleVerseWithTranslations(verse.sura, verse.ayah)
                                    val text = repository.formatShareableVerseText(fullVerse)
                                    shareText(text, "Share Ayah ${verse.sura}:${verse.ayah}")
                                }
                            },
                            onShareImage = {
                                verseForImageShare = verse
                            },
                            onClearSelection = { selectedVerse = null }
                        )
                    }
                }

                // Audio Recitation Bar
                AnimatedVisibility(visible = showChrome || audioState.isPlaying || audioState.isPaused) {
                    AudioBar(
                        audioController = audioController,
                        currentPage = currentPage,
                        onOpenQariSelector = { showQariSheet = true }
                    )
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.backgroundColor)
                .padding(padding)
        ) {
            val isWideScreen = maxWidth >= 700.dp

            // RTL HorizontalPager so swiping right-to-left advances pages naturally like an Arabic Mushaf
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pageNumber = pageIndex + 1
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        when (settings.readerViewMode) {
                            ReaderViewMode.MUSHAF_PAGE -> {
                                if (settings.isDualPageEnabled) {
                                    // Dual-Page RTL spread: Right side is odd/current page, Left side is next page
                                    val rightPage = if (pageNumber % 2 == 1) pageNumber else (pageNumber - 1).coerceAtLeast(1)
                                    val leftPage = (rightPage + 1).coerceAtMost(totalPages)
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            if (leftPage != rightPage) {
                                                MushafPageSurface(
                                                    repository = repository,
                                                    quranInfo = quranInfo,
                                                    page = leftPage,
                                                    themeColors = themeColors,
                                                    showOverlayInfo = settings.showPageInfoOverlay,
                                                    nightTextBrightness = settings.nightModeTextBrightness,
                                                    nightBgBrightness = settings.nightModeBackgroundBrightness,
                                                    appearance = settings.pageAppearance,
                                                    bookmarks = bookmarks,
                                                    selectedVerse = selectedVerse,
                                                    audioPlayingVerse = audioState.currentVerse,
                                                    onToggleChrome = { showChrome = !showChrome },
                                                    onSelectVerse = { selectedVerse = it },
                                                    onOpenDataPrompt = onOpenDataPrompt
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(themeColors.headerFooterColor.copy(alpha = 0.25f))
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            MushafPageSurface(
                                                repository = repository,
                                                quranInfo = quranInfo,
                                                page = rightPage,
                                                themeColors = themeColors,
                                                showOverlayInfo = settings.showPageInfoOverlay,
                                                nightTextBrightness = settings.nightModeTextBrightness,
                                                nightBgBrightness = settings.nightModeBackgroundBrightness,
                                                appearance = settings.pageAppearance,
                                                bookmarks = bookmarks,
                                                selectedVerse = selectedVerse,
                                                audioPlayingVerse = audioState.currentVerse,
                                                onToggleChrome = { showChrome = !showChrome },
                                                onSelectVerse = { selectedVerse = it },
                                                onOpenDataPrompt = onOpenDataPrompt
                                            )
                                        }
                                    }
                                } else {
                                    MushafPageSurface(
                                        repository = repository,
                                        quranInfo = quranInfo,
                                        page = pageNumber,
                                        themeColors = themeColors,
                                        showOverlayInfo = settings.showPageInfoOverlay,
                                        nightTextBrightness = settings.nightModeTextBrightness,
                                        nightBgBrightness = settings.nightModeBackgroundBrightness,
                                        appearance = settings.pageAppearance,
                                        bookmarks = bookmarks,
                                        selectedVerse = selectedVerse,
                                        audioPlayingVerse = audioState.currentVerse,
                                        onToggleChrome = { showChrome = !showChrome },
                                        onSelectVerse = { selectedVerse = it },
                                        onOpenDataPrompt = onOpenDataPrompt
                                    )
                                }
                            }

                            ReaderViewMode.TRANSLATION -> {
                                TranslationPageSurface(
                                    repository = repository,
                                    page = pageNumber,
                                    themeColors = themeColors,
                                    arabicFontSize = settings.arabicFontSize,
                                    translationFontSize = settings.translationFontSize,
                                    showArabicText = settings.showArabicInTranslation,
                                    selectedVerse = selectedVerse,
                                    audioPlayingVerse = audioState.currentVerse,
                                    onSelectVerse = { selectedVerse = it },
                                    onOpenTranslationsManager = onOpenTranslationsManager
                                )
                            }

                            ReaderViewMode.SPLIT_MUSHAF_AND_TRANSLATION -> {
                                if (isWideScreen) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            TranslationPageSurface(
                                                repository = repository,
                                                page = pageNumber,
                                                themeColors = themeColors,
                                                arabicFontSize = settings.arabicFontSize,
                                                translationFontSize = settings.translationFontSize,
                                                showArabicText = settings.showArabicInTranslation,
                                                selectedVerse = selectedVerse,
                                                audioPlayingVerse = audioState.currentVerse,
                                                onSelectVerse = { selectedVerse = it },
                                                onOpenTranslationsManager = onOpenTranslationsManager
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(themeColors.headerFooterColor.copy(alpha = 0.25f))
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            MushafPageSurface(
                                                repository = repository,
                                                quranInfo = quranInfo,
                                                page = pageNumber,
                                                themeColors = themeColors,
                                                showOverlayInfo = settings.showPageInfoOverlay,
                                                nightTextBrightness = settings.nightModeTextBrightness,
                                                nightBgBrightness = settings.nightModeBackgroundBrightness,
                                                appearance = settings.pageAppearance,
                                                bookmarks = bookmarks,
                                                selectedVerse = selectedVerse,
                                                audioPlayingVerse = audioState.currentVerse,
                                                onToggleChrome = { showChrome = !showChrome },
                                                onSelectVerse = { selectedVerse = it },
                                                onOpenDataPrompt = onOpenDataPrompt
                                            )
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            MushafPageSurface(
                                                repository = repository,
                                                quranInfo = quranInfo,
                                                page = pageNumber,
                                                themeColors = themeColors,
                                                showOverlayInfo = settings.showPageInfoOverlay,
                                                nightTextBrightness = settings.nightModeTextBrightness,
                                                nightBgBrightness = settings.nightModeBackgroundBrightness,
                                                appearance = settings.pageAppearance,
                                                bookmarks = bookmarks,
                                                selectedVerse = selectedVerse,
                                                audioPlayingVerse = audioState.currentVerse,
                                                onToggleChrome = { showChrome = !showChrome },
                                                onSelectVerse = { selectedVerse = it },
                                                onOpenDataPrompt = onOpenDataPrompt
                                            )
                                        }
                                        HorizontalDivider()
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            TranslationPageSurface(
                                                repository = repository,
                                                page = pageNumber,
                                                themeColors = themeColors,
                                                arabicFontSize = settings.arabicFontSize,
                                                translationFontSize = settings.translationFontSize,
                                                showArabicText = settings.showArabicInTranslation,
                                                selectedVerse = selectedVerse,
                                                audioPlayingVerse = audioState.currentVerse,
                                                onSelectVerse = { selectedVerse = it },
                                                onOpenTranslationsManager = onOpenTranslationsManager
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPageThemeSheet) {
        PageThemeBottomSheet(
            repository = repository,
            onDismiss = { showPageThemeSheet = false }
        )
    }

    if (showQariSheet) {
        QariListSheet(
            audioController = audioController,
            onDismiss = { showQariSheet = false }
        )
    }

    if (showQuickTafsirSheet && selectedVerse != null) {
        QuickTranslationPreviewSheet(
            repository = repository,
            verse = selectedVerse!!,
            onOpenTranslationsManager = {
                showQuickTafsirSheet = false
                onOpenTranslationsManager()
            },
            onDismiss = { showQuickTafsirSheet = false }
        )
    }

    showTagDialogForVerse?.let { verse ->
        val pageForAyah = quranInfo.getPageFromSuraAyah(verse.sura, verse.ayah)
        val existingBm = bookmarks.find { it.sura == verse.sura && it.ayah == verse.ayah }
        var selectedIds by remember(existingBm) {
            mutableStateOf(existingBm?.tags?.toSet() ?: emptySet())
        }
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTagDialogForVerse = null },
            title = { Text("Tag Ayah ${verse.sura}:${verse.ayah}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    androidx.compose.material3.OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Or create new tag") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bmId = existingBm?.id
                            ?: repository.bookmarksHandler.addOrGetAyahBookmark(verse.sura, verse.ayah, pageForAyah)
                        val finalTagIds = selectedIds.toMutableSet()
                        if (newTagName.isNotBlank()) {
                            val newId = repository.bookmarksHandler.addTag(newTagName.trim())
                            if (newId > 0) finalTagIds.add(newId)
                        }
                        repository.bookmarksHandler.setBookmarkTags(bmId, finalTagIds)
                        showTagDialogForVerse = null
                    }
                ) {
                    Text("Save Tags")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialogForVerse = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    verseForImageShare?.let { verse ->
        AyahImageCardDialog(
            repository = repository,
            verse = verse,
            onDismiss = { verseForImageShare = null }
        )
    }
}

@Composable
private fun MushafPageSurface(
    repository: QuranRepository,
    quranInfo: QuranInfo,
    page: Int,
    themeColors: PageThemeColors,
    showOverlayInfo: Boolean,
    nightTextBrightness: Int,
    nightBgBrightness: Int,
    appearance: PageAppearance,
    bookmarks: List<Bookmark>,
    selectedVerse: SuraAyah?,
    audioPlayingVerse: SuraAyah?,
    onToggleChrome: () -> Unit,
    onSelectVerse: (SuraAyah?) -> Unit,
    onOpenDataPrompt: () -> Unit
) {
    val mushafType = repository.getCurrentMushafType()
    val imagePath = remember(page, mushafType) {
        repository.getPageImageFilePath(page, mushafType)
    }

    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, page, imagePath) {
        value = withContext(Dispatchers.IO) {
            decodeImageBitmapFromFile(imagePath)
        }
    }

    val ayahCoordinates by produceState<AyahCoordinates?>(initialValue = null, page, mushafType) {
        value = repository.getAyahCoordinates(page, mushafType)
    }

    val colorFilter = remember(appearance, nightTextBrightness, nightBgBrightness) {
        buildMushafImageColorFilter(appearance, nightTextBrightness, nightBgBrightness)
    }

    val bookmarkedVersesOnPage = remember(bookmarks, page) {
        bookmarks.filter { it.page == page && !it.isPageBookmark && it.sura != null && it.ayah != null }
            .map { SuraAyah(it.sura!!, it.ayah!!) }
            .toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundColor)
    ) {
        if (showOverlayInfo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Juz' ${quranInfo.getJuzFromPage(page)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.headerFooterColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = QuranDisplayData.getSuraNameFromPage(quranInfo, page),
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.headerFooterColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = imageBitmap
            if (bitmap != null) {
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                val intrinsicWidth = bitmap.width.toFloat().coerceAtLeast(1f)
                val intrinsicHeight = bitmap.height.toFloat().coerceAtLeast(1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                        .pointerInput(ayahCoordinates, containerSize, bitmap, selectedVerse) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val hitVerse = resolveTappedAyah(
                                        tapOffset = tapOffset,
                                        containerSize = containerSize,
                                        bitmapWidth = intrinsicWidth,
                                        bitmapHeight = intrinsicHeight,
                                        coordinates = ayahCoordinates
                                    )
                                    if (selectedVerse != null) {
                                        if (hitVerse != null && hitVerse != selectedVerse) {
                                            onSelectVerse(hitVerse)
                                        } else {
                                            onSelectVerse(null)
                                        }
                                    } else {
                                        onToggleChrome()
                                    }
                                },
                                onLongPress = { pressOffset ->
                                    val hitVerse = resolveTappedAyah(
                                        tapOffset = pressOffset,
                                        containerSize = containerSize,
                                        bitmapWidth = intrinsicWidth,
                                        bitmapHeight = intrinsicHeight,
                                        coordinates = ayahCoordinates
                                    )
                                    if (hitVerse != null) {
                                        onSelectVerse(hitVerse)
                                    }
                                }
                            )
                        }
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Quran Page $page",
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Draw Ayah coordinate highlights (Bookmarked, Audio Active, Selected)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val coords = ayahCoordinates?.glyphCoordinates ?: return@Canvas
                        if (size.width <= 0f || size.height <= 0f) return@Canvas

                        val scale = minOf(size.width / intrinsicWidth, size.height / intrinsicHeight)
                        val drawnWidth = intrinsicWidth * scale
                        val drawnHeight = intrinsicHeight * scale
                        val offsetX = (size.width - drawnWidth) / 2f
                        val offsetY = (size.height - drawnHeight) / 2f

                        fun drawVerseBounds(verseKey: String, highlightColor: Color) {
                            val boundsList = coords[verseKey] ?: return
                            boundsList.forEach { b ->
                                val r = b.bounds
                                drawRect(
                                    color = highlightColor,
                                    topLeft = Offset(
                                        x = offsetX + r.left * scale,
                                        y = offsetY + r.top * scale
                                    ),
                                    size = Size(
                                        width = r.width * scale,
                                        height = r.height * scale
                                    )
                                )
                            }
                        }

                        // 1. Bookmarked verses on this page
                        bookmarkedVersesOnPage.forEach { bmVerse ->
                            drawVerseBounds(bmVerse.key, AyahBookmarkHighlight)
                        }

                        // 2. Currently playing audio verse
                        audioPlayingVerse?.let { audioVerse ->
                            drawVerseBounds(audioVerse.key, AyahAudioHighlight)
                        }

                        // 3. Currently selected verse
                        selectedVerse?.let { selVerse ->
                            drawVerseBounds(selVerse.key, AyahSelectionHighlight)
                        }
                    }
                }
            } else {
                // Page image is not yet downloaded locally
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Page $page image is not yet downloaded for ${mushafType.displayName}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = themeColors.primaryTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenDataPrompt) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download Mushaf Bundle")
                    }
                }
            }
        }

        if (showOverlayInfo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$page (${QuranDisplayData.toArabicNumber(page)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.headerFooterColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun resolveTappedAyah(
    tapOffset: Offset,
    containerSize: IntSize,
    bitmapWidth: Float,
    bitmapHeight: Float,
    coordinates: AyahCoordinates?
): SuraAyah? {
    val map = coordinates?.glyphCoordinates ?: return null
    if (containerSize.width <= 0 || containerSize.height <= 0) return null

    val scale = minOf(
        containerSize.width.toFloat() / bitmapWidth,
        containerSize.height.toFloat() / bitmapHeight
    )
    val drawnWidth = bitmapWidth * scale
    val drawnHeight = bitmapHeight * scale
    val offsetX = (containerSize.width - drawnWidth) / 2f
    val offsetY = (containerSize.height - drawnHeight) / 2f

    val imageX = (tapOffset.x - offsetX) / scale
    val imageY = (tapOffset.y - offsetY) / scale

    for ((key, boundsList) in map) {
        for (b in boundsList) {
            if (b.bounds.contains(imageX, imageY)) {
                val parts = key.split(":")
                if (parts.size == 2) {
                    val s = parts[0].toIntOrNull()
                    val a = parts[1].toIntOrNull()
                    if (s != null && a != null) return SuraAyah(s, a)
                }
            }
        }
    }
    return null
}

@Composable
private fun TranslationPageSurface(
    repository: QuranRepository,
    page: Int,
    themeColors: PageThemeColors,
    arabicFontSize: Float,
    translationFontSize: Float,
    showArabicText: Boolean,
    selectedVerse: SuraAyah?,
    audioPlayingVerse: SuraAyah?,
    onSelectVerse: (SuraAyah?) -> Unit,
    onOpenTranslationsManager: () -> Unit
) {
    val settings by repository.settingsRepository.settings.collectAsState()
    val verses by produceState<List<VerseWithTranslations>>(
        initialValue = emptyList(),
        page,
        settings.mushafType,
        settings.activeTranslationFileNames
    ) {
        value = repository.getVersesForPage(page, settings.mushafType)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundColor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (settings.activeTranslationFileNames.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColors.cardSurfaceColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Active Translations Enabled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primaryTextColor
                            )
                            Text(
                                text = "Download and enable one or more translations or tafaseer to view them alongside the Arabic verses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.secondaryTextColor
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = onOpenTranslationsManager) {
                            Text("Translations")
                        }
                    }
                }
            }
        }

        items(verses, key = { "${it.sura}:${it.ayah}" }) { item ->
            val currentSuraAyah = SuraAyah(item.sura, item.ayah)
            val isSelected = selectedVerse == currentSuraAyah
            val isAudioPlaying = audioPlayingVerse == currentSuraAyah

            // Surah header banner if ayah == 1
            if (item.ayah == 1) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${QuranDisplayData.getSuraName(item.sura, true)} (${QuranDisplayData.getSuraTranslation(item.sura)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "سورة ${QuranDisplayData.getSuraArabicName(item.sura)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectVerse(if (isSelected) null else currentSuraAyah)
                    }
                    .then(
                        if (isSelected || isAudioPlaying) {
                            Modifier.border(
                                width = 2.dp,
                                color = if (isAudioPlaying) QuranGoldAccent else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.cardSurfaceColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${item.sura}:${item.ayah}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.primaryTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (showArabicText && item.arabicText.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${item.arabicText} ﴿${QuranDisplayData.toArabicNumber(item.ayah)}﴾",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = arabicFontSize.sp,
                                lineHeight = (arabicFontSize * 1.65f).sp,
                                textDirection = TextDirection.Rtl
                            ),
                            color = themeColors.primaryTextColor,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item.translations.forEach { (translatorName, qText) ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = translatorName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = qText.text,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = translationFontSize.sp,
                                lineHeight = (translationFontSize * 1.45f).sp
                            ),
                            color = themeColors.primaryTextColor
                        )
                        if (!qText.extraData.isNullOrBlank()) {
                            var showFootnote by remember { mutableStateOf(false) }
                            TextButton(onClick = { showFootnote = !showFootnote }) {
                                Text(if (showFootnote) "Hide Footnotes" else "Show Footnotes")
                            }
                            if (showFootnote) {
                                Text(
                                    text = qText.extraData,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = themeColors.secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AyahSelectionActionBar(
    verse: SuraAyah,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onTagClick: () -> Unit,
    onPlayFromHere: () -> Unit,
    onQuickTafsir: () -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit,
    onShareImage: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected: ${QuranDisplayData.getSuraName(verse.sura, true)} (${verse.sura}:${verse.ayah})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearSelection) {
                    Text("Close")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = onPlayFromHere,
                    label = { Text("Play") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onBookmarkToggle,
                    label = { Text(if (isBookmarked) "Bookmarked" else "Bookmark") },
                    leadingIcon = {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                AssistChip(
                    onClick = onTagClick,
                    label = { Text("Tag") },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onQuickTafsir,
                    label = { Text("Tafsir / Trans") },
                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onCopyText,
                    label = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onShareText,
                    label = { Text("Share Text") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onShareImage,
                    label = { Text("Share Image") },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageThemeBottomSheet(
    repository: QuranRepository,
    onDismiss: () -> Unit
) {
    val settings by repository.settingsRepository.settings.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Page Theme & Display",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ReaderAppearanceSettings(repository = repository)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickTranslationPreviewSheet(
    repository: QuranRepository,
    verse: SuraAyah,
    onOpenTranslationsManager: () -> Unit,
    onDismiss: () -> Unit
) {
    val verseData by produceState<VerseWithTranslations?>(initialValue = null, verse) {
        value = repository.getSingleVerseWithTranslations(verse.sura, verse.ayah)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${QuranDisplayData.getSuraName(verse.sura, true)} (${verse.sura}:${verse.ayah})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onOpenTranslationsManager) {
                    Text("Manage Translations")
                }
            }

            val data = verseData
            if (data == null) {
                CircularProgressIndicator()
            } else {
                if (data.arabicText.isNotBlank()) {
                    Text(
                        text = "${data.arabicText} ﴿${QuranDisplayData.toArabicNumber(verse.ayah)}﴾",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (data.translations.isEmpty()) {
                    Text(
                        text = "No active translations enabled. Tap 'Manage Translations' to download and enable translations or tafaseer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    data.translations.forEach { (translator, qText) ->
                        HorizontalDivider()
                        Text(
                            text = translator,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = qText.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahImageCardDialog(
    repository: QuranRepository,
    verse: SuraAyah,
    onDismiss: () -> Unit
) {
    val verseData by produceState<VerseWithTranslations?>(initialValue = null, verse) {
        value = repository.getSingleVerseWithTranslations(verse.sura, verse.ayah)
    }
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Ayah Image Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Styled offscreen-capturable card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .background(QuranTealDark, RoundedCornerShape(16.dp))
                        .border(2.dp, QuranGoldAccent, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    val data = verseData
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = QuranDisplayData.getSuraName(verse.sura, true),
                                color = QuranGoldAccent,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سورة ${QuranDisplayData.getSuraArabicName(verse.sura)} • ${verse.sura}:${verse.ayah}",
                                color = QuranGoldAccent,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (data != null && data.arabicText.isNotBlank()) {
                            Text(
                                text = "${data.arabicText} ﴿${QuranDisplayData.toArabicNumber(verse.ayah)}﴾",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    lineHeight = 36.sp,
                                    textDirection = TextDirection.Rtl
                                ),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        data?.translations?.firstOrNull()?.let { (translator, qText) ->
                            Text(
                                text = qText.text,
                                color = Color(0xFFE0F2F1),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "— $translator",
                                color = QuranGoldAccent,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                statusText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap()
                        val pngBytes = withContext(Dispatchers.Default) {
                            encodeImageBitmapToPngBytes(bitmap)
                        }
                        if (pngBytes != null) {
                            val fileName = "quran_${verse.sura}_${verse.ayah}.png"
                            val saved = shareOrSavePngImage(
                                pngBytes,
                                fileName,
                                "${QuranDisplayData.getSuraName(verse.sura, true)} (${verse.sura}:${verse.ayah})"
                            )
                            statusText = saved ?: "Shared image card!"
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Share / Save PNG")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
