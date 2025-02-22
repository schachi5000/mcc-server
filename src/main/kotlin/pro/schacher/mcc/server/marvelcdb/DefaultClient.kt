package pro.schacher.mcc.server.marvelcdb

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val DefaultClient = HttpClient(CIO) {
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