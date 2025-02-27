package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardsRequestDto(val cardCodes: List<String>)
