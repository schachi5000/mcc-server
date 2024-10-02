package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(val message: String)