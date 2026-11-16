package at.modulento.app.data.api

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the Retrofit service for a (user-configurable) server URL and turns
 * every failure into an [ApiException].
 */
class ApiClient(private val http: OkHttpClient = defaultHttpClient()) {

    private var cached: Pair<String, ModulentoApi>? = null

    /** [serverUrl] is a normalized URL without trailing slash (see ServerUrl). */
    @Synchronized
    fun api(serverUrl: String): ModulentoApi {
        cached?.let { (url, api) -> if (url == serverUrl) return api }
        val api = Retrofit.Builder()
            .baseUrl("$serverUrl/")
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()
            .create(ModulentoApi::class.java)
        cached = serverUrl to api
        return api
    }

    /** Runs one API call and unwraps the envelope. */
    suspend fun <T : Any> call(block: suspend () -> Envelope<T>): T {
        val envelope = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw httpError(e)
        } catch (e: IOException) {
            throw ApiException.Network(e)
        } catch (e: SerializationException) {
            throw ApiException.InvalidResponse(e)
        } catch (e: IllegalArgumentException) {
            // Malformed base URL or a body that is not JSON at all.
            throw ApiException.InvalidResponse(e)
        }
        envelope.error?.let { throw ApiException.Http(200, it.code, it.message) }
        return envelope.data ?: throw ApiException.InvalidResponse()
    }

    private fun httpError(e: HttpException): ApiException.Http {
        val status = e.code()
        val error = try {
            e.response()?.errorBody()?.string()
                ?.let { json.decodeFromString<Envelope<OkData>>(it).error }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null // SerializationException is an IllegalArgumentException
        }
        return ApiException.Http(
            status = status,
            code = error?.code?.takeIf { it.isNotBlank() } ?: "http_$status",
            message = error?.message.orEmpty(),
        )
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

        fun bearer(token: String) = "Bearer $token"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
