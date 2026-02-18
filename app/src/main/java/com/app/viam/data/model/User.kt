package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val role: String,
    val email: String?,
    val mobile: String?,
    val address: String?,
    val avatar: String?,
    val permissions: List<Permission> = emptyList(),
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
) {
    fun isAdmin(): Boolean = role == "admin"
    fun hasPermission(permissionName: String): Boolean = permissions.any { it.name == permissionName }
}
