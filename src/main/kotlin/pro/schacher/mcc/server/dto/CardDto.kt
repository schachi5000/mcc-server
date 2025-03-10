package pro.schacher.mcc.server.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardDto(
    val attack: Int?,
    val baseThreatFixed: Boolean,
    val cardSetCode: String?,
    val cardSetName: String?,
    val code: String,
    val deckLimit: Int?,
    val defense: Int?,
    val doubleSided: Boolean,
    val escalationThreatFixed: Boolean,
    val factionCode: String,
    val factionName: String,
    val flavor: String?,
    val handSize: Int?,
    val cost: Int?,
    val health: Int?,
    val healthPerHero: Boolean,
    val hidden: Boolean,
    val unique: Boolean,
    val linkedCard: CardDto?,
    val name: String,
    val packCode: String,
    val packName: String,
    val position: Int,
    val quantity: Int,
    val text: String?,
    val boostText: String?,
    val attackText: String?,
    val threatFixed: Boolean,
    val thwart: Int?,
    val traits: String?,
    val type: String?,
    val primaryColor: String?,
    val secondaryColor: String?,
)