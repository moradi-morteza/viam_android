package com.app.viam.data.repository

import com.app.viam.data.model.ApiError
import com.app.viam.data.model.PaginatedParts
import com.app.viam.data.model.Part
import com.app.viam.data.model.PartCategory
import com.app.viam.data.model.PartRequest
import com.app.viam.data.remote.NetworkModule
import com.google.gson.Gson
import java.io.IOException

class PartRepository {

    private val api = NetworkModule.apiService
    private val gson = Gson()

    suspend fun getParts(
        search: String? = null,
        page: Int = 1,
        perPage: Int = 20
    ): AuthResult<PaginatedParts> {
        return try {
            val response = api.getParts(
                search = search?.ifBlank { null },
                page = page,
                perPage = perPage
            )
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت لیست قطعات")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun createPart(request: PartRequest): AuthResult<Part> {
        return try {
            val response = api.createPart(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> {
                    val apiError = parseError(response.errorBody()?.string())
                    AuthResult.Error(apiError?.message ?: "اطلاعات وارد شده معتبر نیست", apiError?.errors)
                }
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در ایجاد قطعه")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun updatePart(id: Int, request: PartRequest): AuthResult<Part> {
        return try {
            val response = api.updatePart(id, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> {
                    val apiError = parseError(response.errorBody()?.string())
                    AuthResult.Error(apiError?.message ?: "اطلاعات وارد شده معتبر نیست", apiError?.errors)
                }
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 404 -> AuthResult.Error("قطعه یافت نشد")
                else -> AuthResult.Error("خطا در ویرایش قطعه")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun deletePart(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deletePart(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 404 -> AuthResult.Error("قطعه یافت نشد")
                response.code() == 422 -> AuthResult.Error("قطعه در جعبه‌ها استفاده شده است")
                else -> AuthResult.Error("خطا در حذف قطعه")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun getPartCategories(): AuthResult<List<PartCategory>> {
        return try {
            val response = api.getPartCategoriesAll()
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت دسته‌بندی‌ها")
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
