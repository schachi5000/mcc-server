package pro.schacher.mcc.server.marvelcdb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpHeaders.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
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
import pro.schacher.mcc.server.dto.DeckDto
import pro.schacher.mcc.server.dto.PackDto
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

class MarvelCDbDataSource(private val serviceUrl: String) {

    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpCache) {}
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

        HttpResponseValidator {
            validateResponse { response ->
                when (response.status) {
                    HttpStatusCode.OK -> return@validateResponse
                    HttpStatusCode.Unauthorized -> throw AuthorizationException()
                    else -> throw RemoteServiceException(
                        statusCode = response.status,
                        message = "Remote service returned status code ${response.status}"
                    )
                }
            }
        }
    }

    private val allCardsCache = mutableMapOf<String, List<CardDto>>()

    suspend fun getAllCards() = withContextSafe {
        val allPacks = getAllPacks().getOrThrow()

        return@withContextSafe allPacks.map {
            async {
                getCardsInPack(it.code).getOrThrow()
            }
        }.awaitAll().flatten()
    }

    suspend fun getAllPacks() = withContextSafe {
        httpClient.get("$serviceUrl/api/public/packs")
            .body<List<PackDto>>()
    }

    suspend fun getCardsInPack(packCode: String) = withContextSafe {
        allCardsCache[packCode]?.let {
            return@withContextSafe it
        }

        httpClient.get("$serviceUrl/api/public/cards/$packCode")
            .body<List<MarvelCdbCard>>()
            .map { it.toCardDto() }
            .also {
                allCardsCache[packCode] = it
            }
    }

    suspend fun getCards(cardSetCode: String) = withContextSafe {
        getAllCards().getOrThrow()
            .distinct()
            .filter { it.cardSetCode == cardSetCode }
    }

    suspend fun getCard(cardCode: String) = withContextSafe {
        httpClient.get("$serviceUrl/api/public/card/$cardCode")
            .body<MarvelCdbCard>()
            .toCardDto()
    }

    suspend fun getSpotlightDecksByDate(date: String) = withContextSafe {
        httpClient.get("$serviceUrl/api/public/decklists/by_date/${date}.json") {
            headers {
                append(CacheControl, "no-store")
            }
        }
            .body<List<MarvelCdbDeck>>()
            .map { it.toDeckDto() }
    }

    suspend fun getCardImage(cardCode: String) = withContextSafe() {
        httpClient.get("$serviceUrl/bundles/cards/${cardCode}.png").bodyAsBytes()
    }

    suspend fun getDeck(deckId: String, bearerToken: String) = withContextSafe {
        httpClient.get("$serviceUrl/api/oauth2/deck/load/$deckId") {
            headers {
                append(Authorization, bearerToken)
                append(CacheControl, "no-store")
            }
        }
            .body<MarvelCdbDeck>()
            .toDeckDto()
    }

    suspend fun getAllUserDecks(bearerToken: String) = withContextSafe {
        httpClient.get("$serviceUrl/api/oauth2/decks") {
            headers {
                append(Authorization, bearerToken)
                append(CacheControl, "no-store")
            }
        }
            .body<List<MarvelCdbDeck>>()
            .map { it.toDeckDto() }
    }

    suspend fun createDeck(heroCardCode: String, deckName: String?, bearerToken: String) =
        withContextSafe {
            httpClient.post("$serviceUrl/api/oauth2/deck/new") {
                headers { append(Authorization, bearerToken) }
                parameter("investigator", heroCardCode)
                parameter("name", deckName)
            }
                .body<MarvelCdbResponse>()
                .msg?.jsonPrimitive?.int ?: throw Exception("Deck creation failed")
        }

    suspend fun updateDeck(deckId: String, slots: String, bearerToken: String) =
        withContextSafe {
            httpClient.put("$serviceUrl/api/oauth2/deck/save/${deckId}") {
                headers { append(Authorization, bearerToken) }
                parameter("slots", slots)
            }
            Unit
        }

    suspend fun deleteDeck(deckId: String, bearerToken: String)=
        withContextSafe {
            httpClient.delete("$serviceUrl/api/oauth2/deck/delete/${deckId}") {
                headers { append(Authorization, bearerToken) }
            }
            Unit
        }
}

private suspend fun <T> withContextSafe(
    context: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> T
): Result<T> = withContext(context) {
    runCatching<T> {
        block()
    }
}

class AuthorizationException() :
    RemoteServiceException(HttpStatusCode.Unauthorized, "Token invalid or expired")

open class RemoteServiceException(
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    message: String? = null,
) : IOException(message) {
    val error: String = "Remote Service Error"
}

@Serializable
internal data class MarvelCdbResponse(
    val success: Boolean,
    val msg: JsonElement?
)

@Serializable
internal data class MarvelCdbError(
    val code: Int,
    val message: String
)

@Serializable
private data class MarvelCdbDeck(
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

private fun MarvelCdbDeck.toDeckDto(): DeckDto = DeckDto(
    createdOn = this.date_creation,
    updatedOn = this.date_update,
    description = this.description_md?.takeIfNotBlank(),
    id = this.id,
    heroCode = this.investigator_code,
    heroName = this.investigator_name,
    name = this.name,
    slots = runCatching { this.slots?.toMap() }.getOrNull(),
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

private fun JsonElement.toMap(): Map<String, Int>? = this.jsonObject
    .mapNotNull { (key, value) -> key to value.jsonPrimitive.int }
    .toMap()
    .takeIf { it.isNotEmpty() }

@Serializable
private data class MarvelCdbCard(
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

private fun MarvelCdbCard.toCardDto(): CardDto = CardDto(
    attack = this.attack,
    baseThreatFixed = this.base_threat_fixed,
    cardSetCode = this.card_set_code,
    cardSetName = this.card_set_code,
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
    unique = this.is_unique,
    linkedCard = this.linked_card?.copy(
        linked_card = null,
        linked_to_code = null,
        linked_to_name = null
    )?.toCardDto(),
    name = this.name,
    packCode = this.pack_code,
    packName = this.pack_name,
    position = this.position,
    quantity = this.quantity,
    text = this.text,
    boostText = this.boost_text,
    attackText = this.attack_text,
    threatFixed = this.threat_fixed,
    thwart = this.thwart,
    traits = this.traits,
    type = this.type_code,
    primaryColor = this.meta?.colors?.getOrNull(0),
    secondaryColor = this.meta?.colors?.getOrNull(1),
)