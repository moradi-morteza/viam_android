package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class Zone(
    val id: Int,
    val name: String,
    val description: String?,
    val shelves: List<Shelf>? = null
)

data class Shelf(
    val id: Int,
    @SerializedName("zone_id") val zoneId: Int,
    val name: String,
    val description: String?,
    val zone: Zone? = null,
    val rows: List<Row>? = null
)

data class Row(
    val id: Int,
    @SerializedName("shelf_id") val shelfId: Int,
    val name: String,
    val description: String?,
    @SerializedName("boxes_count") val boxesCount: Int? = null,
    val shelf: Shelf? = null
)

data class Box(
    val id: Int,
    @SerializedName("row_id") val rowId: Int,
    @SerializedName("part_id") val partId: Int?,
    val code: String,
    val quantity: Double,
    val description: String?,
    val row: Row? = null,
    val part: Part? = null,
    val transactions: List<BoxTransaction>? = null
)

data class BoxTransaction(
    val id: Int,
    @SerializedName("box_id") val boxId: Int,
    @SerializedName("user_id") val userId: Int,
    val type: String,
    val amount: Double,
    @SerializedName("balance_after") val balanceAfter: Double,
    val reference: String?,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    val user: User? = null
)

data class PaginatedBoxes(
    val data: List<Box>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int,
    @SerializedName("per_page") val perPage: Int
)
