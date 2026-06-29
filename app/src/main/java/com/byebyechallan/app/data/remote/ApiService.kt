package com.byebyechallan.app.data.remote

import com.byebyechallan.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Direct mapping of every endpoint in api-docs.json (ByeByeChallan Backend API).
 * Paths and field names match the Swagger spec exactly as of the version we received.
 * If the backend changes a path or field name, update it here first - everything
 * else in the app depends on this contract.
 */
interface ApiService {

    // ---------- Auth Manager ----------

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<okhttp3.ResponseBody>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // ---------- Profile Controller ----------

    @GET("api/v1/user/{userId}/profile")
    suspend fun getAllProfiles(@Path("userId") userId: Long): Response<List<ProfileDto>>

    @POST("api/v1/user/{userId}/profile")
    suspend fun createProfile(
        @Path("userId") userId: Long,
        @Body request: ProfileRequestDto
    ): Response<ProfileDto>

    // ---------- Document Controller ----------

    @GET("api/v1/document/{userId}/profile/{profileId}/registration/{vehicleRegistrationNo}")
    suspend fun getAllDocuments(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long,
        @Path("vehicleRegistrationNo") vehicleRegistrationNo: String
    ): Response<List<UserDocumentDto>>

    @POST("api/v1/document/{userId}/profile/{profileId}/registration/{vehicleRegistrationNo}")
    suspend fun saveDocument(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long,
        @Path("vehicleRegistrationNo") vehicleRegistrationNo: String,
        @Body request: DocumentRequestDto
    ): Response<UserDocumentDto>

    @GET("api/v1/document/list")
    suspend fun getDocumentList(
        @Query("country") country: String,
        @Query("state") state: String,
        @Query("registration_type") registrationType: String,
        @Query("vehicle_type") vehicleType: String,
        @Query("doc_type") docType: String? = null
    ): Response<List<CoreDocumentEntity>>

    // NOTE: Not yet present in the Swagger spec you shared - backend team needs to add
    // this multipart endpoint. The app sends the raw file here first, then uses the
    // returned s3Link in a follow-up call to saveDocument() above. See README.
    @Multipart
    @POST("api/v1/document/upload")
    suspend fun uploadDocumentFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    // ---------- Master Controller ----------

    @GET("api/v1/master/country")
    suspend fun getCountries(): Response<List<CountryDto>>

    @GET("api/v1/master/country/{countryId}")
    suspend fun getStates(@Path("countryId") countryId: String): Response<List<StateDto>>

    @GET("api/v1/master/country/{countryId}/registration")
    suspend fun getRegistrationTypes(@Path("countryId") countryId: String): Response<List<RegistrationDto>>
}
