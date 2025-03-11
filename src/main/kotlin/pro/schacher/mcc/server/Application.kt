package pro.schacher.mcc.server

import io.ktor.http.CacheControl
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.marvelcdb.DefaultClient
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.UrlProvider
import pro.schacher.mcc.server.plugins.configureRouting
import pro.schacher.mcc.server.plugins.routes.cardImageRateLimiter
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

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
    install(Compression) {
        gzip()
        deflate()
    }
    install(CachingHeaders) {
        options { _, _ ->
            CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 6.hours.inWholeSeconds.toInt()))
        }
    }
    install(RateLimit) {
        global {
            rateLimiter(limit = 200, refillPeriod = 1.seconds)
        }

        register(cardImageRateLimiter) {
            rateLimiter(limit = 100, refillPeriod = 1.seconds)
        }
    }
    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, status ->
            val retryAfter = call.response.headers["Retry-After"]
            call.respondText(
                text = "429: Too many requests. Wait for $retryAfter seconds.",
                status = status
            )
        }
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            explicitNulls = false
        })
    }
    configureRouting(MarvelCDbDataSource(UrlProvider(), DefaultClient))
}
