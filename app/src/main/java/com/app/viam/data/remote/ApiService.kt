package com.app.viam.data.remote

import com.app.viam.data.model.Box
import com.app.viam.data.model.BoxTransaction
import com.app.viam.data.model.CreateStaffRequest
import com.app.viam.data.model.LoginRequest
import com.app.viam.data.model.LoginResponse
import com.app.viam.data.model.PaginatedBoxes
import com.app.viam.data.model.PaginatedParts
import com.app.viam.data.model.Part
import com.app.viam.data.model.PartRequest
import com.app.viam.data.model.UpdateStaffRequest
import com.app.viam.data.model.User
import com.app.viam.data.model.BoxRequest
import com.app.viam.data.model.Row
import com.app.viam.data.model.RowRequest
import com.app.viam.data.model.Shelf
import com.app.viam.data.model.ShelfRequest
import com.app.viam.data.model.TransactionRequest
import com.app.viam.data.model.Zone
import com.app.viam.data.model.ZoneRequest
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
    suspend fun getParts(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<PaginatedParts>

    @GET("parts/{id}")
    suspend fun getPart(@Path("id") id: Int): Response<Part>

    @POST("parts")
    suspend fun createPart(@Body request: PartRequest): Response<Part>

    @PUT("parts/{id}")
    suspend fun updatePart(@Path("id") id: Int, @Body request: PartRequest): Response<Part>

    @DELETE("parts/{id}")
    suspend fun deletePart(@Path("id") id: Int): Response<Map<String, String>>

    // --- Warehouse ---

    @GET("warehouse/tree")
    suspend fun getWarehouseTree(): Response<List<Zone>>

    @GET("zones")
    suspend fun getZones(): Response<List<Zone>>

    @POST("zones")
    suspend fun createZone(@Body request: ZoneRequest): Response<Zone>

    @GET("shelves")
    suspend fun getShelves(@Query("zone_id") zoneId: Int? = null): Response<List<Shelf>>

    @POST("shelves")
    suspend fun createShelf(@Body request: ShelfRequest): Response<Shelf>

    @GET("rows")
    suspend fun getRows(@Query("shelf_id") shelfId: Int? = null): Response<List<Row>>

    @POST("rows")
    suspend fun createRow(@Body request: RowRequest): Response<Row>

    @POST("boxes")
    suspend fun createBox(@Body request: BoxRequest): Response<Box>

    @GET("boxes")
    suspend fun getBoxes(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("zone_id") zoneId: Int? = null,
        @Query("row_id") rowId: Int? = null
    ): Response<PaginatedBoxes>

    @GET("boxes/{id}")
    suspend fun getBox(@Path("id") id: Int): Response<Box>

    @GET("boxes/{boxId}/transactions")
    suspend fun getBoxTransactions(
        @Path("boxId") boxId: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): Response<Map<String, Any>>

    @POST("boxes/{boxId}/transactions")
    suspend fun createTransaction(
        @Path("boxId") boxId: Int,
        @Body request: TransactionRequest
    ): Response<BoxTransaction>
}
