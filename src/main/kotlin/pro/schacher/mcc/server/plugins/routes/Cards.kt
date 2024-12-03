package pro.schacher.mcc.server.plugins.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.plugins.getPathParameterOrThrow
import pro.schacher.mcc.server.plugins.runAndHandleErrors
import kotlin.collections.set

private val imageCache = mutableMapOf<String, ByteArray>()

private const val PREFIX = "/api/v1/cards"

internal fun Routing.cards(marvelCDbDataSource: MarvelCDbDataSource) {
    get(PREFIX) {
        call.respond(marvelCDbDataSource.getAllCards())
    }

    get("$PREFIX/{cardCode}") {
        runAndHandleErrors(call) {
            val cardCode = call.getPathParameterOrThrow("cardCode")
            val card = marvelCDbDataSource.getCard(cardCode)
            call.respond(card)
        }
    }

    get("$PREFIX/{cardCode}/image") {
        runAndHandleErrors(call) {
            val cardCode = call.getPathParameterOrThrow("cardCode")
            val card = marvelCDbDataSource.getCard(cardCode)


            imageCache[cardCode]?.let {
                println("Retrieving card image for $cardCode from cache")
                call.respond(it)
                return@runAndHandleErrors
            }

            val result = marvelCDbDataSource.getCardImage(cardCode)
            imageCache[cardCode] = result.getOrThrow()
            call.respond(result.getOrThrow())
        }
    }
}