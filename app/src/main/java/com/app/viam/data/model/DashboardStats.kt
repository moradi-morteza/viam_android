package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class DashboardStats(
    val personnel: PersonnelStats? = null,
    val warehouse: WarehouseStats? = null,
    val parts: PartsStats? = null,
    val transactions: TransactionStats? = null,
    @SerializedName("recent_transactions") val recentTransactions: List<RecentTransaction>? = null,
    @SerializedName("low_stock") val lowStock: List<LowStockBox>? = null,
    @SerializedName("empty_boxes") val emptyBoxes: Int? = null
)

data class PersonnelStats(
    val total: Int,
    val admins: Int
)

data class WarehouseStats(
    val zones: Int,
    val shelves: Int,
    val rows: Int,
    val boxes: Int
)

data class PartsStats(
    val total: Int,
    @SerializedName("total_stock") val totalStock: Int
)

data class TransactionStats(
    val today: Int,
    @SerializedName("this_week") val thisWeek: Int,
    @SerializedName("this_month") val thisMonth: Int,
    val total: Int,
    @SerializedName("by_type") val byType: TransactionByType? = null
)

data class TransactionByType(
    val `in`: Int,
    val out: Int,
    val adjust: Int
)

data class RecentTransaction(
    val id: Int,
    val type: String,
    val amount: Double,
    @SerializedName("box_code") val boxCode: String,
    @SerializedName("part_name") val partName: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("created_at") val createdAt: String
)

data class LowStockBox(
    val id: Int,
    val code: String,
    val quantity: Double,
    @SerializedName("part_name") val partName: String,
    val location: String
)
