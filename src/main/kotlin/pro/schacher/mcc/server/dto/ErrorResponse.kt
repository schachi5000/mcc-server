package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val message: String)