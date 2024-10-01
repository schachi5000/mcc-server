package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

fun Routing.decks(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/decks") {
        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null) {
            call.respond(HttpStatusCode.BadRequest, "AuthHeader missing")
            return@get
        }

        val allPacks = marvelCDbDataSource.getAllUserDecks(authHeader)
        call.respond(allPacks)
    }
}
