package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource
import pro.schacher.mcc.server.dto.CreateDeckRequestDto
import pro.schacher.mcc.server.dto.ErrorResponseDto

private const val PREFIX = "/api/v1/decks"

fun Routing.decks(marvelCDbDataSource: MarvelCDbDataSource) {
    get("$PREFIX/spotlight") {
        val date = call.queryParameters["date"]
        if (date == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(HttpStatusCode.BadRequest, "No pack found")
            )
            return@get
        }

        val allPacks = marvelCDbDataSource.getSpotlightDecksByDate(date)
        call.respond(allPacks)
    }

    get("$PREFIX/{deckId}") {
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

        val deck = marvelCDbDataSource.getDeck(deckId, authHeader)
        call.respond(deck)
    }

    put("$PREFIX/{deckId}") {
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

    post(PREFIX) {
        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null) {
            call.respond(HttpStatusCode.BadRequest, "AuthHeader missing")
            return@post
        }

        val requestDto = call.receive(CreateDeckRequestDto::class)
        val result = marvelCDbDataSource.createDeck(
            requestDto.heroCardCode,
            requestDto.deckName,
            authHeader
        )

        call.respond(result)
    }

    get(PREFIX) {
        val authHeader = call.request.headers["Authorization"]
        if (authHeader == null) {
            call.respond(HttpStatusCode.BadRequest, "AuthHeader missing")
            return@get
        }

        val allPacks = marvelCDbDataSource.getAllUserDecks(authHeader)
        call.respond(allPacks)
    }
}