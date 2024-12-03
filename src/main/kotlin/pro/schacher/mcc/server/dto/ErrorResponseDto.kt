package pro.schacher.mcc.server.dto

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: String,
    val message: String? = null
) {
    constructor(httpStatusCode: HttpStatusCode, message: String? = null) :
            this(httpStatusCode.description, message)
}