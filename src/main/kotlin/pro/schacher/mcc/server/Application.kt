package pro.schacher.mcc.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource
import pro.schacher.mcc.server.plugins.configureRouting

fun main(args: Array<String>) {
    embeddedServer(
        Netty,
        port = 8080
    ) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                explicitNulls = false
            })
        }
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureRouting(MarvelCDbDataSource())
}
