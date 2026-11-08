package com.jetbrains.kmpapp

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.data.model.SuraAyah
import com.jetbrains.kmpapp.repository.AudioPlaybackController
import com.jetbrains.kmpapp.repository.QuranRepository
import com.jetbrains.kmpapp.ui.audio.AudioManagerScreen
import com.jetbrains.kmpapp.ui.dataprompt.QuranDataScreen
import com.jetbrains.kmpapp.ui.home.QuranHomeScreen
import com.jetbrains.kmpapp.ui.pageselect.PageSelectScreen
import com.jetbrains.kmpapp.ui.reader.PagerScreen
import com.jetbrains.kmpapp.ui.search.SearchScreen
import com.jetbrains.kmpapp.ui.settings.SettingsScreen
import com.jetbrains.kmpapp.ui.theme.QuranTheme
import com.jetbrains.kmpapp.ui.translations.TranslationManagerScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object DataPromptRoute

@Serializable
object HomeRoute

@Serializable
data class PagerRoute(
    val initialPage: Int = 1,
    val highlightSura: Int = -1,
    val highlightAyah: Int = -1
)

@Serializable
object PageSelectRoute

@Serializable
object TranslationManagerRoute

@Serializable
object AudioManagerRoute

@Serializable
object SearchRoute

@Serializable
object SettingsRoute

@Composable
fun App() {
    val repository: QuranRepository = koinInject()
    val audioController: AudioPlaybackController = koinInject()
    val settings by repository.settingsRepository.settings.collectAsState()

    val startDestination: Any = if (repository.isMushafBundleReady(settings.mushafType)) {
        HomeRoute
    } else {
        DataPromptRoute
    }

    QuranTheme {
        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<DataPromptRoute> {
                    QuranDataScreen(
                        repository = repository,
                        mushafType = settings.mushafType,
                        onBundleReady = {
                            navController.navigate(HomeRoute) {
                                popUpTo(DataPromptRoute) { inclusive = true }
                            }
                        },
                        onOpenMushafSelector = {
                            navController.navigate(PageSelectRoute)
                        }
                    )
                }

                composable<HomeRoute> {
                    QuranHomeScreen(
                        repository = repository,
                        onOpenPage = { page, verse ->
                            if (!repository.isMushafBundleReady(settings.mushafType)) {
                                navController.navigate(DataPromptRoute)
                            } else {
                                navController.navigate(
                                    PagerRoute(
                                        initialPage = page,
                                        highlightSura = verse?.sura ?: -1,
                                        highlightAyah = verse?.ayah ?: -1
                                    )
                                )
                            }
                        },
                        onOpenSearch = { navController.navigate(SearchRoute) },
                        onOpenMushafSelector = { navController.navigate(PageSelectRoute) },
                        onOpenTranslationsManager = { navController.navigate(TranslationManagerRoute) },
                        onOpenAudioManager = { navController.navigate(AudioManagerRoute) },
                        onOpenSettings = { navController.navigate(SettingsRoute) }
                    )
                }

                composable<PagerRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<PagerRoute>()
                    val highlight = if (route.highlightSura > 0 && route.highlightAyah > 0) {
                        SuraAyah(route.highlightSura, route.highlightAyah)
                    } else {
                        null
                    }
                    PagerScreen(
                        repository = repository,
                        audioController = audioController,
                        initialPage = route.initialPage,
                        initialHighlightVerse = highlight,
                        onOpenTranslationsManager = { navController.navigate(TranslationManagerRoute) },
                        onOpenDataPrompt = { navController.navigate(DataPromptRoute) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<PageSelectRoute> {
                    PageSelectScreen(
                        repository = repository,
                        onMushafSelected = { selectedType ->
                            if (!repository.isMushafBundleReady(selectedType)) {
                                navController.navigate(DataPromptRoute) {
                                    popUpTo(PageSelectRoute) { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<TranslationManagerRoute> {
                    TranslationManagerScreen(
                        translationsRepository = repository.translationsRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<AudioManagerRoute> {
                    AudioManagerScreen(
                        audioController = audioController,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<SearchRoute> {
                    SearchScreen(
                        repository = repository,
                        onResultClick = { page, verse ->
                            navController.navigate(
                                PagerRoute(
                                    initialPage = page,
                                    highlightSura = verse.sura,
                                    highlightAyah = verse.ayah
                                )
                            )
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        repository = repository,
                        onOpenMushafSelector = { navController.navigate(PageSelectRoute) },
                        onOpenTranslationsManager = { navController.navigate(TranslationManagerRoute) },
                        onOpenAudioManager = { navController.navigate(AudioManagerRoute) },
                        onOpenDataPrompt = { navController.navigate(DataPromptRoute) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
