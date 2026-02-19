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
