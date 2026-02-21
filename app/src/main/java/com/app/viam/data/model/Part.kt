package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class PartCategory(
    val id: Int,
    val name: String,
    val description: String?,
    @SerializedName("parts_count") val partsCount: Int = 0
)

data class PartCategoryRequest(
    val name: String,
    val description: String?
)

data class Part(
    val id: Int,
    val sku: String?,
    val name: String,
    val description: String?,
    val unit: String?,
    @SerializedName("total_stock") val totalStock: Int = 0,
    val category: PartCategory? = null
)

data class PartRequest(
    val sku: String?,
    val name: String,
    val description: String?,
    val unit: String?,
    @SerializedName("part_category_id") val partCategoryId: Int?
)

data class PaginatedParts(
    val data: List<Part>,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int,
    @SerializedName("per_page") val perPage: Int
)
