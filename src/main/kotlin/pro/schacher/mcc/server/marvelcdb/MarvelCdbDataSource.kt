package pro.schacher.mcc.server.marvelcdb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pro.schacher.mcc.server.dto.CardDto
import pro.schacher.mcc.server.dto.CreateDeckResponseDto
import pro.schacher.mcc.server.dto.DeckDto
import pro.schacher.mcc.server.dto.DeckUpdateResponseDto
import pro.schacher.mcc.server.dto.PackDto
import kotlin.collections.set

class MarvelCDbDataSource(private val serviceUrl: String) {
    private val allCardsCache = mutableMapOf<String, List<CardDto>>()

    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpCache) {

        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.INFO
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    suspend fun getAllPacks(): List<PackDto> = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/packs")
            .body<List<PackDto>>()
    }

    suspend fun getAllCards(): List<CardDto> = withContext(Dispatchers.IO) {
        val allPacks = getAllPacks()

        return@withContext allPacks.map {
            async {
                getCardsInPack(it.code)
            }
        }.awaitAll().flatten()
    }

    suspend fun getCardsInPack(packCode: String): List<CardDto> = withContext(Dispatchers.IO) {
        allCardsCache[packCode]?.let {
            return@withContext it
        }

        httpClient.get("$serviceUrl/pack/$packCode")
            .body<List<MarvelCdbCard>>()
            .map { it.toCardDto() }
            .also {
                allCardsCache[packCode] = it
            }
    }

    suspend fun getCard(cardCode: String): CardDto = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/card/$cardCode")
            .body<MarvelCdbCard>()
            .toCardDto()
    }

    suspend fun getSpotlightDecksByDate(date: String): List<DeckDto> = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/spotlight/${date}")
            .body<List<MarvelCdbDeck>>()
            .map { it.toDeckDto() }
    }

    suspend fun getCardImage(cardCode: String): Result<ByteArray> = runCatching {
        val response = httpClient.get("$serviceUrl/image/$cardCode")
        if (response.status != HttpStatusCode.OK) {
            throw throw RemoteServiceException(
                response.status,
                "Could not load image for $cardCode"
            )
        }

        response.bodyAsBytes()
    }

    suspend fun getDeck(deckId: String, authToken: String): DeckDto = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/api/oauth2/deck/load/$deckId") {
            headers { append("Authorization", authToken) }
        }.body<MarvelCdbDeck>().toDeckDto()
    }

    suspend fun getAllUserDecks(authToken: String): List<DeckDto> = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/api/oauth2/decks") {
            headers { append("Authorization", authToken) }
        }.also {
            if (it.status == HttpStatusCode.Unauthorized) {
                throw RemoteServiceException(it.status, "Unauthorized")
            }
        }
            .body<List<MarvelCdbDeck>>()
            .map { it.toDeckDto() }
    }

    suspend fun createDeck(heroCardCode: String, deckName: String?, authToken: String) =
        withContext(Dispatchers.IO) {
            httpClient.post("$serviceUrl/api/oauth2/deck/new") {
                headers { append("Authorization", authToken) }
                parameter("investigator", heroCardCode)
                parameter("name", deckName)
            }.body<CreateDeckResponseDto>()
        }

    suspend fun updateDeck(deckId: String, slots: String, authToken: String):
            DeckUpdateResponseDto = withContext(Dispatchers.IO) {
        httpClient.put("$serviceUrl/api/oauth2/deck/save/${deckId}") {
            headers { append("Authorization", authToken) }
            parameter("slots", slots)
        }.body<DeckUpdateResponseDto>()
    }
}

class RemoteServiceException(statusCode: HttpStatusCode, message: String) :
    IOException("[${statusCode.value}] $message")

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

internal fun MarvelCdbDeck.toDeckDto(): DeckDto = DeckDto(
    createdOn = this.date_creation,
    updatedOn = this.date_update,
    description = this.description_md?.takeIfNotBlank(),
    id = this.id,
    heroCode = this.investigator_code,
    heroName = this.investigator_name,
    name = this.name,
    slots = this.slots?.toMap(),
    tags = this.tags?.takeIfNotBlank(),
    userId = this.user_id,
    version = this.version,
    problem = this.problem?.takeIfNotBlank(),
    aspect = this.meta?.parseAspect()
)

private const val LEADERSHIP = "leadership"
private const val JUSTICE = "justice"
private const val AGGRESSION = "aggression"
private const val PROTECTION = "protection"

private fun String.parseAspect(): String? = when {
    this.contains(JUSTICE) -> JUSTICE
    this.contains(LEADERSHIP) -> LEADERSHIP
    this.contains(AGGRESSION) -> AGGRESSION
    this.contains(PROTECTION) -> PROTECTION
    else -> null
}


private fun String.takeIfNotBlank(): String? = this.ifBlank { null }

private fun JsonElement.toMap(): Map<String, Int>? = try {
    this.jsonObject.map { (key, value) ->
        key to value.jsonPrimitive.int
    }.toMap()
} catch (e: Exception) {
    println("Could not convert map for $this")
    null
}

@Serializable
internal data class MarvelCdbCard(
    val attack: Int?,
    val base_threat_fixed: Boolean,
    val card_set_code: String?,
    val card_set_name: String?,
    val card_set_type_name_code: String?,
    val code: String,
    val deck_limit: Int?,
    val defense: Int?,
    val double_sided: Boolean,
    val escalation_threat_fixed: Boolean,
    val faction_code: String,
    val faction_name: String,
    val flavor: String?,
    val hand_size: Int?,
    val cost: Int?,
    val health: Int?,
    val health_per_hero: Boolean,
    val hidden: Boolean,
    val imagesrc: String?,
    val is_unique: Boolean,
    val linked_card: MarvelCdbCard?,
    val linked_to_code: String?,
    val linked_to_name: String?,
    val meta: Meta?,
    val name: String,
    val octgn_id: String?,
    val pack_code: String,
    val pack_name: String,
    val permanent: Boolean,
    val position: Int,
    val quantity: Int,
    val real_name: String?,
    val real_text: String?,
    val real_traits: String?,
    val text: String?,
    val boost_text: String?,
    val attack_text: String?,
    val threat_fixed: Boolean,
    val thwart: Int?,
    val traits: String?,
    val type_code: String?,
    val type_name: String?,
    val url: String
) {
    @Serializable
    data class Meta(
        val colors: List<String>,
        val offset: String?
    )
}

internal fun MarvelCdbCard.toCardDto(): CardDto = CardDto(
    attack = this.attack,
    baseThreatFixed = this.base_threat_fixed,
    cardSetCode = this.card_set_code,
    cardSetName = this.card_set_code,
    cardSetTypeNameCode = this.card_set_type_name_code,
    code = this.code,
    deckLimit = this.deck_limit,
    defense = this.defense,
    doubleSided = this.double_sided,
    escalationThreatFixed = this.escalation_threat_fixed,
    factionCode = this.faction_code,
    factionName = this.faction_name,
    flavor = this.flavor,
    handSize = this.hand_size,
    cost = this.cost,
    health = this.health,
    healthPerHero = this.health_per_hero,
    hidden = this.hidden,
    imageSrc = this.imagesrc,
    unique = this.is_unique,
    linkedCard = this.linked_card?.copy(
        linked_card = null,
        linked_to_code = null,
        linked_to_name = null
    )?.toCardDto(),
    linkedCardCode = this.linked_to_code,
    linkedCardName = this.linked_to_name,
    name = this.name,
    octagonId = this.octgn_id,
    packCode = this.pack_code,
    packName = this.pack_name,
    permanent = this.permanent,
    position = this.position,
    quantity = this.quantity,
    realName = this.real_name,
    realText = this.real_text,
    realTraits = this.real_traits,
    text = this.text,
    boostText = this.boost_text,
    attackText = this.attack_text,
    threatFixed = this.threat_fixed,
    thwart = this.thwart,
    traits = this.traits,
    typeCode = this.type_code,
    typeName = this.type_name,
    url = this.url,
)