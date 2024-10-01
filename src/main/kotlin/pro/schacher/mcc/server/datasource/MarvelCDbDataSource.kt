package pro.schacher.mcc.server.datasource

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
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
import pro.schacher.mcc.server.dto.PackDto
import java.time.LocalDate

class MarvelCDbDataSource {
    private val serviceUrl = "https://3ipbqpd2fj.execute-api.eu-north-1.amazonaws.com"

    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
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
        httpClient.get("$serviceUrl/packs").also {
            println(it.bodyAsText())
        }.body<List<PackDto>>()
    }

    suspend fun getCardsInPack(packCode: String): List<CardDto> {
        return emptyList()
    }

    suspend fun getCard(cardCode: String): CardDto {
        throw IOException()
    }

    suspend fun getSpotlightDecksByDate(date: LocalDate): List<DeckDto> {
        return emptyList()
    }

    suspend fun getCardImage(cardCode: String): Result<HttpResponse> = kotlin.runCatching {
        val response = httpClient.get("$serviceUrl/image/$cardCode")

        if (response.status != HttpStatusCode.OK) {
            throw throw RemoteServiceException(response.status, "Could not load image for $cardCode")
        }

        response
    }
}

class RemoteServiceException(val statusCode: HttpStatusCode, message: String) : IOException(message)