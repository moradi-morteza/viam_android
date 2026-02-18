package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class CreateStaffRequest(
    val name: String,
    val username: String,
    val password: String,
    val email: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    @SerializedName("role_id") val roleId: Int? = null,
    val permissions: List<Int> = emptyList()
)

data class UpdateStaffRequest(
    val name: String,
    val username: String,
    val password: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    @SerializedName("role_id") val roleId: Int? = null,
    val permissions: List<Int> = emptyList()
)
