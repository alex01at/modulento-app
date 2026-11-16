package at.modulento.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.modulento.app.ModulentoApp
import at.modulento.app.R
import at.modulento.app.data.ModulentoRepository
import at.modulento.app.data.api.Meta
import at.modulento.app.ui.theme.ModulentoTheme

/**
 * The app has three phases, decided by stored state and the API alone:
 * no server yet -> guest (public website + login) -> logged in (tabs).
 * Each phase owns its own navigation graph, so switching phase never leaves
 * stale screens on a back stack.
 */
@Composable
fun AppRoot() {
    val repository = (LocalContext.current.applicationContext as ModulentoApp).repository
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory { initializer { AppViewModel(repository, ::clearWebSession) } },
    )
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val metaState = state.meta
    val serverUrl = state.serverUrl

    ModulentoTheme(accentHex = (metaState as? MetaState.Loaded)?.meta?.accent) {
        Surface {
            when {
                serverUrl == null -> ServerUrlScreen(
                    currentUrl = null,
                    loggedIn = false,
                    onSave = appViewModel::saveServerUrl,
                    onSaved = {},
                    onBack = null,
                )

                !state.loggedIn -> GuestNavigation(serverUrl, state.sessionExpired, repository, appViewModel)

                else -> when (metaState) {
                    MetaState.Loading -> LoadingBox()
                    is MetaState.Failed -> MessageBox(
                        title = stringResource(R.string.error_meta_title),
                        text = stringResource(metaState.error.text),
                        primaryLabel = stringResource(R.string.action_retry),
                        onPrimary = appViewModel::refreshMeta,
                        secondaryLabel = stringResource(R.string.action_logout),
                        onSecondary = appViewModel::logout,
                    )
                    is MetaState.Loaded -> MainNavigation(serverUrl, metaState.meta, repository, appViewModel)
                }
            }
        }
    }
}

@Composable
private fun GuestNavigation(
    serverUrl: String,
    sessionExpired: Boolean,
    repository: ModulentoRepository,
    appViewModel: AppViewModel,
) {
    val navController = rememberNavController()

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) navController.navigate(LoginRoute) { launchSingleTop = true }
    }

    NavHost(navController, startDestination = GuestRoute) {
        composable<GuestRoute> {
            GuestScreen(
                serverUrl = serverUrl,
                onLogin = { navController.navigate(LoginRoute) { launchSingleTop = true } },
                onSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
            )
        }
        composable<LoginRoute> {
            val loginViewModel: LoginViewModel = viewModel(
                factory = viewModelFactory { initializer { LoginViewModel(repository) } },
            )
            val loginState by loginViewModel.state.collectAsStateWithLifecycle()
            // The "session expired" note belongs to this one visit of the login screen.
            DisposableEffect(Unit) { onDispose { appViewModel.onSessionExpiredShown() } }
            LoginScreen(
                state = loginState,
                serverUrl = serverUrl,
                sessionExpired = sessionExpired,
                onSubmit = loginViewModel::submit,
                onDone = appViewModel::onLoggedIn,
                onBack = { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            ServerUrlScreen(
                currentUrl = serverUrl,
                loggedIn = false,
                onSave = appViewModel::saveServerUrl,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private class Tab(val route: Any, val label: Int, val icon: ImageVector)

@Composable
private fun MainNavigation(
    serverUrl: String,
    meta: Meta,
    repository: ModulentoRepository,
    appViewModel: AppViewModel,
) {
    val navController = rememberNavController()
    val title = meta.siteName ?: stringResource(R.string.app_name)

    // "Verwaltung" exists only if the API returned an admin menu for this account.
    val tabs = buildList {
        add(Tab(DashboardRoute, R.string.nav_dashboard, Icons.Filled.Home))
        add(Tab(DiscoverRoute, R.string.nav_discover, Icons.Filled.Search))
        if (meta.hasAdminMenu) add(Tab(AdminRoute, R.string.nav_admin, Icons.Filled.Build))
        add(Tab(AccountRoute, R.string.nav_account, Icons.Filled.AccountCircle))
    }
    val destination = navController.currentBackStackEntryAsState().value?.destination
    val onTab = destination == null || tabs.any { destination.hasRoute(it.route::class) }

    fun openWeb(path: String, pageTitle: String) {
        navController.navigate(WebRoute(path, pageTitle)) { launchSingleTop = true }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hasRoute(tab.route::class) == true,
                            onClick = { navController.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    title = title,
                    meta = meta,
                    onRefresh = appViewModel::refreshMeta,
                    onOpenAdmin = { navController.switchTab(AdminRoute) },
                    onOpenDiscover = { navController.switchTab(DiscoverRoute) },
                    onOpenNotice = { notice -> notice.link?.let { openWeb(it, notice.label) } },
                )
            }
            composable<DiscoverRoute> {
                val accountTitle = stringResource(R.string.discover_account_title)
                DiscoverScreen(
                    title = title,
                    serverUrl = serverUrl,
                    onOpenAccount = { openWeb("/account", accountTitle) },
                )
            }
            if (meta.hasAdminMenu) {
                composable<AdminRoute> {
                    AdminScreen(
                        title = title,
                        groups = meta.adminMenu,
                        onOpen = { item -> openWeb(item.path, item.label) },
                    )
                }
            }
            composable<AccountRoute> {
                AccountScreen(
                    title = title,
                    account = meta.account,
                    serverUrl = serverUrl,
                    onSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                    onLogout = appViewModel::logout,
                )
            }
            composable<WebRoute> { entry ->
                val route = entry.toRoute<WebRoute>()
                val handoffViewModel: WebHandoffViewModel = viewModel(
                    factory = viewModelFactory { initializer { WebHandoffViewModel(repository, route.path) } },
                )
                val handoffState by handoffViewModel.state.collectAsStateWithLifecycle()
                WebHandoffScreen(
                    title = route.title,
                    state = handoffState,
                    onRetry = handoffViewModel::request,
                    onSessionRejected = appViewModel::onSessionRejected,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<SettingsRoute> {
                ServerUrlScreen(
                    currentUrl = serverUrl,
                    loggedIn = true,
                    onSave = appViewModel::saveServerUrl,
                    // A changed address ends the session and with it this whole graph.
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Standard bottom-bar behavior: one back stack per tab, no duplicates. */
private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
