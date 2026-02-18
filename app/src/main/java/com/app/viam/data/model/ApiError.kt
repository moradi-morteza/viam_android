package com.app.viam.data.model

data class ApiError(
    val message: String,
    val errors: Map<String, List<String>>? = null
)
