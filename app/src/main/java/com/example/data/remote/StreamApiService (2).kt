package com.example.data.remote

import com.example.data.model.AppNotification
import com.example.data.model.AppUpdateInfo
import com.example.data.model.Episode
import com.example.data.model.HomeResponse
import com.example.data.model.MediaItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface StreamApiService {
  @GET("api/home")
  suspend fun getHome(): HomeResponse

  @GET("api/movies")
  suspend fun getMovies(): List<MediaItem>

  @GET("api/series")
  suspend fun getSeries(): List<MediaItem>

  @GET("api/series/{id}/episodes")
  suspend fun getSeriesEpisodes(@Path("id") seriesId: String): List<Episode>

  @GET("api/search")
  suspend fun search(
    @Query("q") query: String,
    @Query("filter") filter: String? = null
  ): List<MediaItem>

  @GET("api/random")
  suspend fun getRandom(): List<MediaItem>

  @GET("api/notifications")
  suspend fun getNotifications(): List<AppNotification>

  /** Tells the server this phone opened the app (anonymous id), for the admin user count. */
  @POST("api/ping")
  suspend fun ping(@Body body: Map<String, String>): okhttp3.ResponseBody

  /** Counts one view of a title (for the "Trending" and "Most viewed" rows). */
  @POST("api/view")
  suspend fun reportView(@Body body: Map<String, String>): okhttp3.ResponseBody

  @GET("api/update")
  suspend fun getUpdate(): AppUpdateInfo

  companion object {
    fun create(baseUrl: String = ApiConstants.DEFAULT_KOYEB_FQDN): StreamApiService {
      val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

      val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
      }

      val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

      val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

      return Retrofit.Builder()
        .baseUrl(normalizedUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(StreamApiService::class.java)
    }
  }
}
