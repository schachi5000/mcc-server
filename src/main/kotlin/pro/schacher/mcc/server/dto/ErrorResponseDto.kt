package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: String,
    val message: String? = null
)