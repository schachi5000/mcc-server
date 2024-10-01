package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

internal fun Routing.card(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/card/{cardCode}") {
        val cardCode = call.pathParameters["cardCode"]
        if (cardCode == null) {
            call.respond(HttpStatusCode.BadRequest, "No card code")
            return@get
        }

        val card = marvelCDbDataSource.getCard(cardCode)
        call.respond(card)
    }

    get("/card/image/{cardCode}") {
        val cardCode = call.pathParameters["cardCode"]
        if (cardCode == null) {
            call.respond(HttpStatusCode.BadRequest, "No card code")
            return@get
        }

        val result = marvelCDbDataSource.getCardImage(cardCode)
        if (result.isFailure) {
            val throwable = result.exceptionOrNull()!!
            call.respond(HttpStatusCode.InternalServerError, message = throwable.message.toString())
            return@get
        }

        call.respond(result.getOrThrow())

    }
}