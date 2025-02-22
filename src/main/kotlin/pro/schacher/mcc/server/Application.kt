package pro.schacher.mcc.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.marvelcdb.AuthorizationException
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.RemoteServiceException
import pro.schacher.mcc.server.marvelcdb.UrlProvider
import pro.schacher.mcc.server.plugins.configureRouting


fun main(args: Array<String>) {
    println("Starting Server")
    embeddedServer(
        Netty,
        port = 8080
    ) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            explicitNulls = false
        })
    }
    configureRouting(MarvelCDbDataSource(UrlProvider(), DefaultClient))
}

private val DefaultClient = HttpClient(CIO) {
    followRedirects = true
    install(HttpCache) {}
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
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
