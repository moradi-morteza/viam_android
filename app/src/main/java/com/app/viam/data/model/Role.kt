package com.app.viam.data.model

data class Role(
    val id: Int,
    val name: String,
    val permissions: List<Permission> = emptyList()
)
