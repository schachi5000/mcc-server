package pro.schacher.mcc.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

fun Application.configureRouting(marvelCDbDataSource: MarvelCDbDataSource) {
    routing {
        card(marvelCDbDataSource)
        allCards(marvelCDbDataSource)
        allPacks(marvelCDbDataSource)
        cardImage(marvelCDbDataSource)
        spotlight(marvelCDbDataSource)
    }
}

private fun Routing.card(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/card") {
        val parameters = call.parameters
        val cardCode = call.pathParameters["cardCode"]
        if (cardCode == null) {
            call.respond(HttpStatusCode.BadRequest, "No card code")
            return@get
        }

        call.respondText("$cardCode")
    }
}


private fun Routing.allCards(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/allCards") {
        call.respondText("allCards")
    }
}

private fun Routing.allPacks(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/packs") {
        val allPacks = marvelCDbDataSource.getAllPacks()
        call.respond(allPacks)
    }
}

private fun Routing.cardImage(marvelCDbDataSource: MarvelCDbDataSource) {
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

private fun Routing.spotlight(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/spotlight") {

        call.respondText("spotlight")
    }
}