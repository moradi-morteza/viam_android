package com.app.viam.data.repository

import com.app.viam.data.model.Box
import com.app.viam.data.model.BoxRequest
import com.app.viam.data.model.BoxTransaction
import com.app.viam.data.model.PaginatedBoxes
import com.app.viam.data.model.Row
import com.app.viam.data.model.RowRequest
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.ShelfRequest
import com.app.viam.data.model.TransactionRequest
import com.app.viam.data.model.Zone
import com.app.viam.data.model.ZoneRequest
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

    suspend fun getBoxByCode(code: String): AuthResult<Box> {
        return try {
            val response = api.getBoxByCode(code)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 404 -> AuthResult.Error("جعبه با این کد یافت نشد")
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در دریافت اطلاعات جعبه")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    suspend fun getZones(): AuthResult<List<Zone>> {
        return try {
            val response = api.getZones()
            if (response.isSuccessful) AuthResult.Success(response.body()!!)
            else AuthResult.Error("خطا در دریافت ناحیه‌ها")
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun updateZone(id: Int, request: ZoneRequest): AuthResult<Zone> {
        return try {
            val response = api.updateZone(id, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ویرایش ناحیه")
                else -> AuthResult.Error("خطا در ویرایش ناحیه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun deleteZone(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteZone(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در حذف ناحیه")
                else -> AuthResult.Error("خطا در حذف ناحیه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun createZone(request: ZoneRequest): AuthResult<Zone> {
        return try {
            val response = api.createZone(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ایجاد ناحیه")
                else -> AuthResult.Error("خطا در ایجاد ناحیه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun getShelves(zoneId: Int? = null): AuthResult<List<Shelf>> {
        return try {
            val response = api.getShelves(zoneId)
            if (response.isSuccessful) AuthResult.Success(response.body()!!)
            else AuthResult.Error("خطا در دریافت قفسه‌ها")
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun updateShelf(id: Int, request: ShelfRequest): AuthResult<Shelf> {
        return try {
            val response = api.updateShelf(id, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ویرایش قفسه")
                else -> AuthResult.Error("خطا در ویرایش قفسه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun deleteShelf(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteShelf(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در حذف قفسه")
                else -> AuthResult.Error("خطا در حذف قفسه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun createShelf(request: ShelfRequest): AuthResult<Shelf> {
        return try {
            val response = api.createShelf(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ایجاد قفسه")
                else -> AuthResult.Error("خطا در ایجاد قفسه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun getRows(shelfId: Int? = null): AuthResult<List<Row>> {
        return try {
            val response = api.getRows(shelfId)
            if (response.isSuccessful) AuthResult.Success(response.body()!!)
            else AuthResult.Error("خطا در دریافت ردیف‌ها")
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun updateRow(id: Int, request: RowRequest): AuthResult<Row> {
        return try {
            val response = api.updateRow(id, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ویرایش ردیف")
                else -> AuthResult.Error("خطا در ویرایش ردیف")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun deleteRow(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteRow(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در حذف ردیف")
                else -> AuthResult.Error("خطا در حذف ردیف")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun createRow(request: RowRequest): AuthResult<Row> {
        return try {
            val response = api.createRow(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ایجاد ردیف")
                else -> AuthResult.Error("خطا در ایجاد ردیف")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun deleteBox(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteBox(id)
            when {
                response.isSuccessful -> AuthResult.Success(Unit)
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در حذف جعبه")
                else -> AuthResult.Error("خطا در حذف جعبه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun createBox(request: BoxRequest): AuthResult<Box> {
        return try {
            val response = api.createBox(request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> AuthResult.Error(parseErrorMessage(response.errorBody()?.string()) ?: "خطا در ایجاد جعبه")
                else -> AuthResult.Error("خطا در ایجاد جعبه")
            }
        } catch (e: IOException) { AuthResult.NetworkError
        } catch (e: Exception) { AuthResult.Error("خطای غیرمنتظره رخ داد") }
    }

    suspend fun createTransaction(
        boxId: Int,
        request: TransactionRequest
    ): AuthResult<BoxTransaction> {
        return try {
            val response = api.createTransaction(boxId, request)
            when {
                response.isSuccessful -> AuthResult.Success(response.body()!!)
                response.code() == 422 -> {
                    val msg = response.errorBody()?.string()
                        ?.let { parseErrorMessage(it) }
                        ?: "خطا در ثبت تراکنش"
                    AuthResult.Error(msg)
                }
                response.code() == 403 -> AuthResult.Error("دسترسی کافی ندارید")
                else -> AuthResult.Error("خطا در ثبت تراکنش")
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Error("خطای غیرمنتظره رخ داد")
        }
    }

    private fun parseErrorMessage(json: String?): String? {
        if (json == null) return null
        return try {
            val obj = com.google.gson.JsonParser.parseString(json).asJsonObject
            obj.get("message")?.asString
        } catch (_: Exception) { null }
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
