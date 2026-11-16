package at.modulento.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Persistent, secret app state: which server, and the API token for it. */
interface SessionStore {
    var serverUrl: String?
    var token: String?
}

/** Stores both values encrypted (AES-256, key in the Android Keystore). */
@Suppress("DEPRECATION") // security-crypto is in maintenance mode but still the standard helper for this.
class EncryptedSessionStore(context: Context) : SessionStore {

    private val prefs: SharedPreferences = open(context.applicationContext)

    override var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit { putOrRemove(KEY_SERVER_URL, value) }

    override var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit { putOrRemove(KEY_TOKEN, value) }

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: String?) {
        if (value == null) remove(key) else putString(key, value)
    }

    private fun open(context: Context): SharedPreferences = try {
        create(context)
    } catch (_: Exception) {
        // The keystore key can get lost (e.g. after a device restore). The old
        // file is unreadable then; start over instead of crashing on every launch.
        context.deleteSharedPreferences(FILE)
        create(context)
    }

    private fun create(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE = "session"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_TOKEN = "token"
    }
}
