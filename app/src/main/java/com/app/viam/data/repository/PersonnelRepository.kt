package com.app.viam.data.repository

import com.app.viam.data.model.ApiError
import com.app.viam.data.model.CreateStaffRequest
import com.app.viam.data.model.UpdateStaffRequest
import com.app.viam.data.model.User
import com.app.viam.data.remote.NetworkModule
import com.google.gson.Gson
import java.io.IOException

class PersonnelRepository {

    private val api = NetworkModule.apiService
    private val gson = Gson()

    suspend fun getStaffs(): AuthResult<List<User>> {
        return try {
            val response = api.getStaffs()
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت لیست پرسنل")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun getStaff(id: Int): AuthResult<User> {
        return try {
            val response = api.getStaff(id)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 404 -> AuthResult.Error("کاربر یافت نشد")
                else -> AuthResult.Error("خطا در دریافت اطلاعات کاربر")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun createStaff(request: CreateStaffRequest): AuthResult<User> {
        return try {
            val response = api.createStaff(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> {
                    val apiError = parseError(response.errorBody()?.string())
                    AuthResult.Error(
                        message = apiError?.message ?: "اطلاعات وارد شده معتبر نیست",
                        fieldErrors = apiError?.errors
                    )
                }
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در ایجاد کاربر")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun updateStaff(id: Int, request: UpdateStaffRequest): AuthResult<User> {
        return try {
            val response = api.updateStaff(id, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> {
                    val apiError = parseError(response.errorBody()?.string())
                    AuthResult.Error(
                        message = apiError?.message ?: "اطلاعات وارد شده معتبر نیست",
                        fieldErrors = apiError?.errors
                    )
                }
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 404 -> AuthResult.Error("کاربر یافت نشد")
                else -> AuthResult.Error("خطا در ویرایش کاربر")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun deleteStaff(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteStaff(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 404 -> AuthResult.Error("کاربر یافت نشد")
                else -> AuthResult.Error("خطا در حذف کاربر")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    private fun parseError(errorBody: String?): ApiError? {
        return try {
            errorBody?.let { gson.fromJson(it, ApiError::class.java) }
        } catch (e: Exception) {
            null
        }
    }
}
