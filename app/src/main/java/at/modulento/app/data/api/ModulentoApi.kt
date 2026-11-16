package at.modulento.app.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Mobile API v1. Paths are relative (no leading slash) so that a server URL
 * with a sub-directory keeps working.
 */
interface ModulentoApi {

    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("device_name") deviceName: String?,
    ): Envelope<LoginData>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Envelope<OkData>

    @FormUrlEncoded
    @POST("api/v1/auth/webview-handoff")
    suspend fun webviewHandoff(
        @Header("Authorization") authorization: String,
        @Field("path") path: String?,
    ): Envelope<HandoffData>

    @GET("api/v1/meta")
    suspend fun meta(@Header("Authorization") authorization: String): Envelope<Meta>
}
