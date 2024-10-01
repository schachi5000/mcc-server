package pro.schacher.mcc.server.datasource

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.dto.CardDto
import pro.schacher.mcc.server.dto.DeckDto
import pro.schacher.mcc.server.dto.DeckUpdateResponseDto
import pro.schacher.mcc.server.dto.PackDto

class MarvelCDbDataSource {
    private val serviceUrl = "https://3ipbqpd2fj.execute-api.eu-north-1.amazonaws.com"

    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpCache)
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

    suspend fun getCardsInPack(packCode: String): List<CardDto> {
        return httpClient.get("$serviceUrl/pack/$packCode")
            .body<List<CardDto>>()
    }

    suspend fun getCard(cardCode: String): CardDto {
        return httpClient.get("$serviceUrl/card/$cardCode")
            .body<CardDto>()
    }

    suspend fun getSpotlightDecksByDate(date: String): List<DeckDto> {
        return httpClient.get("$serviceUrl/spotlight/${date}")
            .body<List<DeckDto>>()
    }

    suspend fun getCardImage(cardCode: String): Result<ByteArray> = runCatching {
        val response = httpClient.get("$serviceUrl/image/$cardCode")
        if (response.status != HttpStatusCode.OK) {
            throw throw RemoteServiceException(response.status, "Could not load image for $cardCode")
        }

        response.bodyAsBytes()
    }

    suspend fun getDeck(deckId: String, authToken: String): DeckDto =
        httpClient.get("$serviceUrl/api/oauth2/deck/load/$deckId") {
            headers { append("Authorization", authToken) }
        }.body<DeckDto>()

    suspend fun getAllUserDecks(authHeader: String): List<DeckDto> {
        return httpClient.get("$serviceUrl/api/oauth2/decks") {
            headers { append("Authorization", authHeader) }
        }
            .body<List<DeckDto>>()
    }

    suspend fun updateDeck(deckId: String, slots: String, authHeader: String): DeckUpdateResponseDto =
        this.httpClient.put("$serviceUrl/api/oauth2/deck/save/${deckId}") {
            headers { append("Authorization", authHeader) }
            parameter("slots", slots)
        }.body<DeckUpdateResponseDto>()
}

class RemoteServiceException(statusCode: HttpStatusCode, message: String) :
    IOException("[${statusCode.value}] $message")