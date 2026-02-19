package com.app.viam.data.remote

import com.app.viam.data.model.CreateStaffRequest
import com.app.viam.data.model.LoginRequest
import com.app.viam.data.model.LoginResponse
import com.app.viam.data.model.Part
import com.app.viam.data.model.PartRequest
import com.app.viam.data.model.UpdateStaffRequest
import com.app.viam.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Auth ---

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("me")
    suspend fun me(): Response<User>

    // --- Personnel / Staff ---

    @GET("staffs")
    suspend fun getStaffs(): Response<List<User>>

    @GET("staffs/{id}")
    suspend fun getStaff(@Path("id") id: Int): Response<User>

    @POST("staffs")
    suspend fun createStaff(@Body request: CreateStaffRequest): Response<User>

    @PUT("staffs/{id}")
    suspend fun updateStaff(
        @Path("id") id: Int,
        @Body request: UpdateStaffRequest
    ): Response<User>

    @DELETE("staffs/{id}")
    suspend fun deleteStaff(@Path("id") id: Int): Response<Map<String, String>>

    // --- Parts ---

    @GET("parts")
    suspend fun getParts(@Query("search") search: String? = null): Response<List<Part>>

    @GET("parts/{id}")
    suspend fun getPart(@Path("id") id: Int): Response<Part>

    @POST("parts")
    suspend fun createPart(@Body request: PartRequest): Response<Part>

    @PUT("parts/{id}")
    suspend fun updatePart(@Path("id") id: Int, @Body request: PartRequest): Response<Part>

    @DELETE("parts/{id}")
    suspend fun deletePart(@Path("id") id: Int): Response<Map<String, String>>
}
