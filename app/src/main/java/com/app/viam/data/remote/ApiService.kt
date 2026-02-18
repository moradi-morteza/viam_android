package com.app.viam.data.remote

import com.app.viam.data.model.LoginRequest
import com.app.viam.data.model.LoginResponse
import com.app.viam.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("logout")
    suspend fun logout(): Response<Map<String, String>>

    @GET("me")
    suspend fun me(): Response<User>
}
