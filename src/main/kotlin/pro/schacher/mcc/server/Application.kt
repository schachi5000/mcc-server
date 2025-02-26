package pro.schacher.mcc.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.marvelcdb.DefaultClient
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.UrlProvider
import pro.schacher.mcc.server.plugins.configureRouting
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
    install(RateLimit){
        global {
            rateLimiter(limit = 50, refillPeriod = 60.seconds)
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
