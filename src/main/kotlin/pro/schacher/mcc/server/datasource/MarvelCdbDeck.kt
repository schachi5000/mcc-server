package pro.schacher.mcc.server.datasource

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class MarvelCdbDeck(
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