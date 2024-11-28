package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateDeckResponseDto(val success: Boolean, val msg: JsonElement?)
