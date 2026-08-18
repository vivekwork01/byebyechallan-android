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

    @GET("api/v1/user/{userId}/profile/{profileId}")
    suspend fun getProfile(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long
    ): Response<ProfileDto>

    @POST("api/v1/user/{userId}/profile")
    suspend fun createProfile(
        @Path("userId") userId: Long,
        @Body request: ProfileRequestDto
    ): Response<ProfileDto>

    @PUT("api/v1/user/{userId}/profile/{profileId}")
    suspend fun updateProfile(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long,
        @Body request: ProfileRequestDto
    ): Response<ProfileDto>

    // ---------- Document Controller ----------

    @POST("api/v1/user/{userId}/profile/{profileId}/add-vehicle/{vehicleRegistrationNo}")
    suspend fun addVehicleToProfile(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long,
        @Path("vehicleRegistrationNo") vehicleRegistrationNo: String,
        @Body request: VehicleRequestDto
    ): Response<ProfileVehicleResponseDto>

    @GET("api/v1/user/{userId}/profile/{profileId}/all-vehicle")
    suspend fun getVehiclesForProfile(
        @Path("userId") userId: Long,
        @Path("profileId") profileId: Long
    ): Response<List<ProfileVehicleResponseDto>>

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
        @Body request: DocumentDto
    ): Response<UserDocumentDto>

    @GET("api/v1/document/list")
    suspend fun getDocumentList(
        @Query("country") country: String,
        @Query("state") state: String,
        @Query("registration_type") registrationType: String,
        @Query("vehicle_type") vehicleType: String,
        @Query("doc_type") docType: String? = null
    ): Response<List<DocumentRequestDto>>

    @Multipart
    @POST("api/v1/file/{userId}/upload")
    suspend fun uploadDocumentFile(
        @Path("userId") userId: Long,
        @Part file: MultipartBody.Part
    ): Response<FileResponseDto>

    @Streaming
    @GET("api/v1/file/{userId}/{fileName}")
    suspend fun downloadFile(
        @Path("userId") userId: Long,
        @Path("fileName") fileName: String
    ): Response<ResponseBody>

    // ---------- Master Controller ----------

    @GET("api/v1/master/country")
    suspend fun getCountries(): Response<List<CountryDto>>

    @GET("api/v1/master/country/{countryId}")
    suspend fun getStates(@Path("countryId") countryId: String): Response<List<StateDto>>

    @GET("api/v1/master/country/{countryId}/registration")
    suspend fun getRegistrationTypes(@Path("countryId") countryId: String): Response<List<RegistrationDto>>

    @GET("api/v1/master/vehicle-type")
    suspend fun getVehicleTypes(): Response<List<VehicleTypeResponseDto>>
}
