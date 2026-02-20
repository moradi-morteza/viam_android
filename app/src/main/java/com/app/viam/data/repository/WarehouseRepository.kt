package com.app.viam.data.repository

import com.app.viam.data.model.Box
import com.app.viam.data.model.PaginatedBoxes
import com.app.viam.data.model.Zone
import com.app.viam.data.remote.NetworkModule
import java.io.IOException

class WarehouseRepository {

    private val api = NetworkModule.apiService

    suspend fun getBoxes(
        search: String? = null,
        page: Int = 1,
        perPage: Int = 20,
        zoneId: Int? = null,
        rowId: Int? = null
    ): AuthResult<PaginatedBoxes> {
        return try {
            val response = api.getBoxes(
                search = search?.ifBlank { null },
                page = page,
                perPage = perPage,
                zoneId = zoneId,
                rowId = rowId
            )
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت لیست جعبه‌ها")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun getBox(id: Int): AuthResult<Box> {
        return try {
            val response = api.getBox(id)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 404 -> AuthResult.Error("جعبه یافت نشد")
                else -> AuthResult.Error("خطا در دریافت اطلاعات جعبه")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun getWarehouseTree(): AuthResult<List<Zone>> {
        return try {
            val response = api.getWarehouseTree()
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت ساختار انبار")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }
}
