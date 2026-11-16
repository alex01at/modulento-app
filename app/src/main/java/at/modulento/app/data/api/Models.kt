package at.modulento.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Every API response is either `{"data": ...}` or `{"error": {...}}`. */
@Serializable
data class Envelope<T>(
    val data: T? = null,
    val error: ErrorBody? = null,
)

@Serializable
data class ErrorBody(
    val code: String = "",
    val message: String = "",
)

@Serializable
data class Account(
    val id: Int,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    val locale: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("demo_mode") val demoMode: Boolean = false,
)

@Serializable
data class LoginData(
    val token: String,
    val account: Account,
    @SerialName("api_version") val apiVersion: Int = 1,
)

@Serializable
data class OkData(val ok: Boolean = false)

@Serializable
data class HandoffData(
    val code: String = "",
    val url: String,
    @SerialName("expires_in") val expiresIn: Int = 0,
)

@Serializable
data class Meta(
    @SerialName("api_version") val apiVersion: Int = 1,
    @SerialName("core_version") val coreVersion: String = "",
    val account: Account,
    val permissions: List<String> = emptyList(),
    val modules: List<Module> = emptyList(),
    val extensions: List<Extension> = emptyList(),
    @SerialName("admin_menu") val adminMenu: List<AdminGroup> = emptyList(),
    val notices: List<Notice> = emptyList(),
    /** Field set is defined by the server's design settings; kept generic on purpose. */
    val branding: JsonObject = JsonObject(emptyMap()),
    val languages: Languages = Languages(),
) {
    val siteName: String? get() = brandingString("site_name")
    val accent: String? get() = brandingString("accent")
    val hasAdminMenu: Boolean get() = adminMenu.any { it.items.isNotEmpty() }

    private fun brandingString(key: String): String? =
        (branding[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
}

@Serializable
data class Module(
    val id: String,
    @SerialName("label_key") val labelKey: String = "",
    val label: String = "",
    val enabled: Boolean = false,
)

@Serializable
data class Extension(
    val id: String,
    val name: String = "",
    val version: String = "",
    val type: String = "",
    val description: String = "",
)

@Serializable
data class AdminGroup(
    val id: String,
    @SerialName("label_key") val labelKey: String = "",
    val label: String = "",
    val items: List<AdminItem> = emptyList(),
)

@Serializable
data class AdminItem(
    @SerialName("label_key") val labelKey: String = "",
    val label: String = "",
    val path: String,
    val permission: String? = null,
)

@Serializable
data class Notice(
    val count: Int = 0,
    @SerialName("label_key") val labelKey: String = "",
    val label: String = "",
    val link: String? = null,
)

@Serializable
data class Languages(
    val default: String = "",
    val enabled: List<String> = emptyList(),
)
