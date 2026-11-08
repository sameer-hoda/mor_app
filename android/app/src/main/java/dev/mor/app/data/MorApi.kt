package dev.mor.app.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** One-to-one with backend/src/routes.js. */
interface MorApi {
    @GET("api/health")
    suspend fun health(): Health

    @GET("api/profile")
    suspend fun profile(): Profile

    @POST("api/profile")
    suspend fun saveProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Profile

    @GET("api/home")
    suspend fun home(
        @Query("date") date: String? = null,
    ): Home

    @POST("api/meals")
    suspend fun logMeal(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): MealLog

    @GET("api/meals/{id}")
    suspend fun meal(
        @Path("id") id: Long,
    ): MealDetail

    @DELETE("api/meals/{id}")
    suspend fun deleteMeal(
        @Path("id") id: Long,
    ): OkResp

    @GET("api/foods")
    suspend fun foods(
        @Query("q") q: String? = null,
    ): FoodsResp

    @POST("api/foods/estimate")
    suspend fun estimate(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): EstimateResp

    @POST("api/scan")
    suspend fun scan(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): ScanResult

    @GET("api/scans/{id}")
    suspend fun scanDetail(
        @Path("id") id: Long,
    ): ScanDetail

    @POST("api/voice-log")
    suspend fun voiceLog(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): ScanResult

    @POST("api/water")
    suspend fun water(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): WaterResp

    @POST("api/weight")
    suspend fun weight(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): WeightResp

    @GET("api/insights")
    suspend fun insights(@Query("range") range: String = "week"): Insights

    @GET("api/streak")
    suspend fun streak(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
    ): StreakInfo

    @GET("api/challenges")
    suspend fun challenges(): Challenges

    @POST("api/challenges/claim")
    suspend fun claim(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): ClaimResp

    @POST("api/badges/award")
    suspend fun awardBadge(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): ClaimResp

    @POST("api/reset")
    suspend fun reset(): OkResp

    @GET("api/notifications")
    suspend fun notifications(): NotifsResp

    @POST("api/notifications/read")
    suspend fun markRead(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): OkResp
}
