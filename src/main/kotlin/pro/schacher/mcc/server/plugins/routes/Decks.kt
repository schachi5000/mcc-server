package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.dto.CreateDeckRequestDto
import pro.schacher.mcc.server.dto.CreateDeckResponseDto
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.RemoteServiceException
import pro.schacher.mcc.server.plugins.getAcceptLocaleOrDefault
import pro.schacher.mcc.server.plugins.getPathParameterOrThrow
import pro.schacher.mcc.server.plugins.getQueryParameterOrThrow
import pro.schacher.mcc.server.plugins.runAndHandleErrors

private const val PREFIX = "/api/v1/decks"
private const val AUTHORIZATION = "Authorization"

fun Routing.decks(marvelCDbDataSource: MarvelCDbDataSource) {
    get("$PREFIX/spotlight") {
        runAndHandleErrors(call) {
            val date = call.getQueryParameterOrThrow("date")
            val allPacks = marvelCDbDataSource.getSpotlightDecksByDate(date).getOrThrow()
            call.respond(allPacks)
        }
    }

    get(PREFIX) {
        runAndHandleErrors(call) {
            val allDecks = marvelCDbDataSource.getAllUserDecks(call.getBearerToken()).getOrThrow()
            call.respond(allDecks)
        }
    }

    get("$PREFIX/{deckId}") {
        runAndHandleErrors(call) {
            val deckId = call.getPathParameterOrThrow("deckId")
            val deck = marvelCDbDataSource.getDeck(deckId, call.getBearerToken()).getOrThrow()
            call.respond(deck)
        }
    }

    put("$PREFIX/{deckId}") {
        runAndHandleErrors(call) {
            val deckId = call.getPathParameterOrThrow("deckId")
            val slots = call.getQueryParameterOrThrow("slots")

            marvelCDbDataSource.updateDeck(deckId, slots, call.getBearerToken()).getOrThrow()
            call.respond(HttpStatusCode.OK)
        }
    }

    post(PREFIX) {
        runAndHandleErrors(call) {
            val locale = call.getAcceptLocaleOrDefault()
            val requestDto = call.receive(CreateDeckRequestDto::class)

            val heroCard = marvelCDbDataSource.getCard(locale, requestDto.heroCardCode).getOrThrow()
            val heroCards =
                marvelCDbDataSource.getCards(locale, heroCard.cardSetCode!!).getOrThrow()

            val slots = heroCards
                .filter { it.deckLimit != null }
                .filter { it.type != "hero" && it.type != "alter_ego" && it.type != "obligation" }
                .associate { it.code to it.deckLimit!! }
                .let { Json.encodeToString(it) }

            val deckId = marvelCDbDataSource.createDeck(
                heroCard.code,
                requestDto.deckName,
                call.getBearerToken()
            ).getOrThrow()

            marvelCDbDataSource.updateDeck(
                deckId.toString(),
                slots,
                call.getBearerToken()
            )

            call.respond(CreateDeckResponseDto(deckId))
        }
    }

    delete("$PREFIX/{deckId}") {
        runAndHandleErrors(call) {
            val deckId = call.getPathParameterOrThrow("deckId")
            marvelCDbDataSource.deleteDeck(deckId, call.getBearerToken()).getOrThrow()
            call.respond(HttpStatusCode.OK)
        }
    }
}

private fun RoutingCall.getBearerToken(): String = this.request.headers[AUTHORIZATION]
    ?: throw RemoteServiceException(BadRequest, "Bearer token not found")

