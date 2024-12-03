package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeckResponseDto(val success: Boolean,
                                 val deckId: Int? = null,
                                 val error: String? = null)
