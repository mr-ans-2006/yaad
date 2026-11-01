package io.github.mrans2006.yaad.ui

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.components.BottomBarHost
import io.github.mrans2006.yaad.ui.components.YaadTab
import io.github.mrans2006.yaad.ui.screens.DetailActions
import io.github.mrans2006.yaad.ui.screens.DetailScreen
import io.github.mrans2006.yaad.ui.screens.FolderActions
import io.github.mrans2006.yaad.ui.screens.FolderScreen
import io.github.mrans2006.yaad.ui.screens.FoldersActions
import io.github.mrans2006.yaad.ui.screens.FoldersScreen
import io.github.mrans2006.yaad.ui.screens.SearchActions
import io.github.mrans2006.yaad.ui.screens.SearchScreen
import io.github.mrans2006.yaad.ui.theme.LocalReduceMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Yaad's map of screens:
 *
 *   [Search tab]  search ──────────────► shot/{id}  (detail)
 *   [Folders tab] folders ─► folder/{key} ─► shot/{id}
 *                 settings, battery (from the top bar / tips)
 *
 * The bottom bar sits over the two tabs. Each tab keeps its own back stack and scroll position
 * (switching tabs saves and restores them). Everything lives inside one SharedTransitionLayout,
 * so a thumbnail can grow into the detail image and shrink back.
 *
 * MainActivity hands in the things only an Activity can do (open links, the gallery, settings…).
 */
@Composable
fun YaadNavHost(
    state: HomeState,
    search: SearchActions,
    onAdd: () -> Unit,
    detailActions: (onBack: () -> Unit) -> DetailActions,
    observeShot: (Long) -> Flow<Shot?> = { flowOf(null) },
    onQueryUsed: (String) -> Unit = {},
    settings: @Composable (onBack: () -> Unit, onBattery: () -> Unit) -> Unit = { _, _ -> },
    battery: @Composable (onBack: () -> Unit) -> Unit = {},
    nav: NavHostController = rememberNavController(),
) {
    val reduceMotion = LocalReduceMotion.current
    fun openShot(shot: Shot, origin: String) = nav.navigate("shot/${shot.id}?from=${Uri.encode(origin)}")
    val back: () -> Unit = { nav.popBackStack() }

    // Which tab is lit: the tab whose screens are on top of the back stack.
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var tab by rememberSaveable { mutableStateOf(YaadTab.SEARCH) }
    LaunchedEffect(route) {
        when {
            route == "search" -> tab = YaadTab.SEARCH
            route == "folders" || route?.startsWith("folder/") == true -> tab = YaadTab.FOLDERS
        }
    }
    val showBar = route == null || route == "search" || route == "folders" || route.startsWith("folder/")
    fun goTo(target: YaadTab) {
        val dest = if (target == YaadTab.SEARCH) "search" else "folders"
        if (target == tab) {
            // Tapping the lit tab again goes back to its first screen.
            nav.popBackStack(dest, inclusive = false)
            return
        }
        nav.navigate(dest) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BottomBarHost(showBar, tab, ::goTo) {
        SharedTransitionLayout(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalSharedScope provides this) {
                NavHost(
                    nav,
                    startDestination = "search",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { if (reduceMotion) EnterTransition.None else enterFor(initialState.destination.route, targetState.destination.route, forward = true) },
                    exitTransition = { if (reduceMotion) ExitTransition.None else exitFor(initialState.destination.route, targetState.destination.route, forward = true) },
                    popEnterTransition = { if (reduceMotion) EnterTransition.None else enterFor(initialState.destination.route, targetState.destination.route, forward = false) },
                    popExitTransition = { if (reduceMotion) ExitTransition.None else exitFor(initialState.destination.route, targetState.destination.route, forward = false) },
                ) {
                    composable("search") {
                        CompositionLocalProvider(LocalNavScope provides this, LocalShotOrigin provides "search") {
                            SearchScreen(
                                state,
                                search.copy(
                                    onOpen = { onQueryUsed(state.query); openShot(it, "search") },
                                    onSearchDone = { if (!state.results.isNullOrEmpty()) onQueryUsed(state.query) },
                                    onSettings = { nav.navigate("settings") },
                                    onBatteryGuide = { nav.navigate("battery") },
                                    onAdd = onAdd,
                                ),
                            )
                        }
                    }
                    composable("folders") {
                        CompositionLocalProvider(LocalNavScope provides this) {
                            FoldersScreen(
                                state.shots,
                                FoldersActions(
                                    onOpenFolder = { nav.navigate("folder/${Uri.encode(it.key)}") },
                                    onAdd = onAdd,
                                    onSettings = { nav.navigate("settings") },
                                ),
                            )
                        }
                    }
                    composable("folder/{key}", arguments = listOf(navArgument("key") { type = NavType.StringType })) { e ->
                        val key = e.arguments?.getString("key").orEmpty()
                        val folder = remember(state.shots, key) { Folders.find(state.shots, key) }
                        CompositionLocalProvider(LocalNavScope provides this, LocalShotOrigin provides "folder:$key") {
                            FolderScreen(folder, FolderActions(onBack = back, onOpen = { openShot(it, "folder:$key") }))
                        }
                    }
                    composable(
                        "shot/{id}?from={from}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.LongType },
                            navArgument("from") { type = NavType.StringType; defaultValue = "list" },
                        ),
                    ) { e ->
                        val id = e.arguments!!.getLong("id")
                        val from = e.arguments?.getString("from") ?: "list"
                        // Start from the copy already in memory, so the image is there on the very first frame
                        // (the grow-from-thumbnail animation needs it), then follow the database.
                        val initial = remember(id) { state.shots.firstOrNull { it.id == id } }
                        val live by remember(id) { observeShot(id) }.collectAsStateWithLifecycle(initialValue = initial)
                        CompositionLocalProvider(LocalNavScope provides this) {
                            (live ?: initial)?.let { DetailScreen(it, detailActions(back), origin = from) }
                        }
                    }
                    composable("settings") { settings(back) { nav.navigate("battery") } }
                    composable("battery") { battery(back) }
                }
            }
        }
    }
}
