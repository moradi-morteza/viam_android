package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class Part(
    val id: Int,
    val sku: String?,
    val name: String,
    val description: String?,
    val unit: String?,
    @SerializedName("total_stock") val totalStock: Int = 0
)

data class PartRequest(
    val sku: String?,
    val name: String,
    val description: String?,
    val unit: String?
)

data class PaginatedParts(
    val data: List<Part>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int,
    @SerializedName("per_page") val perPage: Int
)
