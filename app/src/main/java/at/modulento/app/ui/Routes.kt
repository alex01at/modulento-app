package at.modulento.app.ui

import kotlinx.serialization.Serializable

// Guest (not logged in)
@Serializable data object GuestRoute
@Serializable data object LoginRoute

// Logged in: the four tabs
@Serializable data object DashboardRoute
@Serializable data object DiscoverRoute
@Serializable data object AdminRoute
@Serializable data object AccountRoute

/** A site page opened with a logged-in web session (WebView handoff). */
@Serializable data class WebRoute(val path: String, val title: String)

@Serializable data object SettingsRoute
