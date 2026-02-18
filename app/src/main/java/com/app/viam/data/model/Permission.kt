package com.app.viam.data.model

import com.google.gson.annotations.SerializedName

data class Permission(
    val id: Int,
    val name: String,
    @SerializedName("guard_name") val guardName: String,
    val category: String?,
    @SerializedName("display_name") val displayName: String?
)
