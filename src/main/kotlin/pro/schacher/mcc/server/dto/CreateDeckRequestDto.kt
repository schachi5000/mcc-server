package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateDeckRequestDto(val heroCardCode: String, val deckName: String?)
