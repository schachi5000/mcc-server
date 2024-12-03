package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pro.schacher.mcc.server.dto.CreateDeckRequestDto
import pro.schacher.mcc.server.dto.CreateDeckResponseDto
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.RemoteServiceException
import pro.schacher.mcc.server.plugins.getPathParameterOrThrow
import pro.schacher.mcc.server.plugins.getQueryParameterOrThrow
import pro.schacher.mcc.server.plugins.runAndHandleErrors

private const val PREFIX = "/api/v1/decks"
private const val AUTHORIZATION = "Authorization"

fun Routing.decks(marvelCDbDataSource: MarvelCDbDataSource) {
    get("$PREFIX/spotlight") {
        runAndHandleErrors(call) {
            val date = call.getQueryParameterOrThrow("date")
            val allPacks = marvelCDbDataSource.getSpotlightDecksByDate(date)
            call.respond(allPacks)
        }
    }

    get("$PREFIX/{deckId}") {
        runAndHandleErrors(call) {
            val deckId = call.getPathParameterOrThrow("deckId")
            val deck = marvelCDbDataSource.getDeck(deckId, call.getBearerToken())
            it.respond(deck)
        }
    }

    put("$PREFIX/{deckId}") {
        runAndHandleErrors(call) {
            val deckId = call.getPathParameterOrThrow("deckId")
            val slots = call.getPathParameterOrThrow("slots")
            val allPacks = marvelCDbDataSource.updateDeck(deckId, slots, call.getBearerToken())
            it.respond(allPacks)
        }
    }

    post(PREFIX) {
        runAndHandleErrors(call) {
            val requestDto = it.receive(CreateDeckRequestDto::class)

            val heroCard = marvelCDbDataSource.getCard(requestDto.heroCardCode)
            val heroCards = marvelCDbDataSource.getCards(heroCard.cardSetCode!!)

            val slots = heroCards
                .filter { it.deckLimit != null }
                .filter { it.type != "hero" && it.type != "alter_ego" && it.type != "obligation" }
                .associate { it.code to it.deckLimit!! }
                .let { Json.encodeToString(it) }

            val result = runCatching {
                marvelCDbDataSource.createDeck(
                    heroCard.code,
                    requestDto.deckName,
                    call.getBearerToken()
                )
            }

            val deckId = result.getOrNull()?.deckId
                ?: throw RemoteServiceException(
                    InternalServerError,
                    result.exceptionOrNull()?.toString() ?: "Failed to create deck"
                )

            marvelCDbDataSource.updateDeck(
                deckId.toString(),
                slots,
                it.getBearerToken()
            )

            it.respond(CreateDeckResponseDto(deckId))
        }
    }

    get(PREFIX) {
        runAndHandleErrors(call) {
            val allPacks = marvelCDbDataSource.getAllUserDecks(call.getBearerToken())
            it.respond(allPacks)
        }
    }
}

private fun RoutingCall.getBearerToken(): String = this.request.headers[AUTHORIZATION]
    ?: throw RemoteServiceException(BadRequest, "Bearer token not found")

