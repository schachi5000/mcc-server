package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

fun Routing.deck(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/deck/{deckId}") {
        val deckId = call.pathParameters["deckId"]
        if (deckId == null) {
            call.respond(HttpStatusCode.BadRequest, "No deck found")
            return@get
        }

        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null) {
            call.respond(HttpStatusCode.BadRequest, "AuthHeader missing")
            return@get
        }

        val allPacks = marvelCDbDataSource.getDeck(deckId, authHeader)
        call.respond(allPacks)
    }

    put("/deck/{deckId}") {
        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null) {
            call.respond(HttpStatusCode.BadRequest, "AuthHeader missing")
            return@put
        }

        val deckId = call.pathParameters["deckId"]
        if (deckId == null) {
            call.respond(HttpStatusCode.BadRequest, "No deck found")
            return@put
        }

        val slots = call.queryParameters["slots"]
        if (slots == null) {
            call.respond(HttpStatusCode.BadRequest, "No slots found")
            return@put
        }

        val allPacks = marvelCDbDataSource.updateDeck(deckId, slots, authHeader)
        call.respond(allPacks)
    }
}