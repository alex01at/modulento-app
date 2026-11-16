package at.modulento.app

import android.app.Application
import at.modulento.app.data.EncryptedSessionStore
import at.modulento.app.data.ModulentoRepository

class ModulentoApp : Application() {
    val repository: ModulentoRepository by lazy {
        ModulentoRepository(EncryptedSessionStore(this))
    }
}
