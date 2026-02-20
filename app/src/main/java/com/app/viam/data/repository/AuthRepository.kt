package com.app.viam.data.repository

import com.app.viam.data.local.UserPreferences
import com.app.viam.data.model.ApiError
import com.app.viam.data.model.LoginRequest
import com.app.viam.data.model.User
import com.app.viam.data.remote.NetworkModule
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.io.IOException

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(
        val message: String,
        val fieldErrors: Map<String, List<String>>? = null
    ) : AuthResult<Nothing>()
    object NetworkError : AuthResult<Nothing>()
}

class AuthRepository(private val userPreferences: UserPreferences) {

    private val api = NetworkModule.apiService
    private val gson = Gson()

    val tokenFlow: Flow<String?> = userPreferences.tokenFlow
    val userFlow: Flow<User?> = userPreferences.userFlow

    suspend fun login(username: String, password: String): AuthResult<User> {
        return try {
            val response = api.login(LoginRequest(username, password))
            when {
                response.isSuccessful -> {
                    val body = response.body()!!
                    userPreferences.saveAuthData(body.token, body.user)
                    NetworkModule.updateToken(body.token)
                    AuthResult.Success(body.user)
                }
                response.code() == 401 || response.code() == 422 -> {
                    val apiError = try {
                        gson.fromJson(response.errorBody()?.string(), ApiError::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    AuthResult.Error(
                        message = apiError?.message ?: "نام کاربری یا رمز عبور اشتباه است",
                        fieldErrors = apiError?.errors
                    )
                }
                else -> AuthResult.Error("خطای سرور. لطفا مجددا تلاش کنید")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun logout(): AuthResult<Unit> {
        return try {
            try {
                api.logout()
            } catch (e: Exception) {
                // Ignore network errors on logout — local clear is what matters
            }
            userPreferences.clearAuthData()
            NetworkModule.updateToken(null)
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error("خطا در خروج از سیستم")
        }
    }

    suspend fun fetchMe(): AuthResult<User> {
        return try {
            val response = api.me()
            when {
                response.isSuccessful -> {
                    val user = response.body()!!
                    userPreferences.saveUser(user)
                    AuthResult.Success(user)
                }
                else -> AuthResult.Error("خطا در دریافت اطلاعات کاربر")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun initializeTokenCache() {
        val token = userPreferences.getToken()
        NetworkModule.updateToken(token)
    }
}
