package pro.schacher.mcc.server.datasource

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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
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

        httpClient.get("$serviceUrl/pack/$packCode").body<List<CardDto>>().also {
            allCardsCache[packCode] = it
        }
    }

    suspend fun getCard(cardCode: String): CardDto = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/card/$cardCode").body<CardDto>()
    }

    suspend fun getSpotlightDecksByDate(date: String): List<DeckDto> = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/spotlight/${date}").body<List<DeckDto>>()
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
        }.body<DeckDto>()
    }

    suspend fun getAllUserDecks(authToken: String): List<DeckDto> = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/api/oauth2/decks") {
            headers { append("Authorization", authToken) }
        }.body<List<DeckDto>>()
    }

    suspend fun getAllUserDecksAsString(authToken: String) = withContext(Dispatchers.IO) {
        httpClient.get("$serviceUrl/api/oauth2/decks") {
            headers { append("Authorization", authToken) }
        }.also {
            val result = it.bodyAsText()
            println(result)
        }.bodyAsText()
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