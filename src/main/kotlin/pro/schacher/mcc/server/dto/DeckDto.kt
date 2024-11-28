package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DeckDto(
    val date_creation: String,
    val date_update: String,
    val description_md: String?,
    val id: Int,
    val investigator_code: String?,
    val investigator_name: String?,
    val meta: String?,
    val name: String,
    val slots: JsonElement?,
    val tags: String?,
    val user_id: Int?,
    val version: String?,
    val problem: String?
)