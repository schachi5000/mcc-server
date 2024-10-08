package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource
import pro.schacher.mcc.server.dto.ErrorResponseDto

private val imageCache = mutableMapOf<String, ByteArray>()

private const val PREFIX = "/api/v1/cards"

internal fun Routing.cards(marvelCDbDataSource: MarvelCDbDataSource) {
    get("$PREFIX/{cardCode}") {
        val cardCode = call.pathParameters["cardCode"]
        if (cardCode == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(HttpStatusCode.BadRequest, "No card code")
            )
            return@get
        }

        val card = marvelCDbDataSource.getCard(cardCode)
        call.respond(card)
    }

    get("$PREFIX/{cardCode}/image") {
        val cardCode = call.pathParameters["cardCode"]
        if (cardCode == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(HttpStatusCode.BadRequest, "No card code")
            )
            return@get
        }

        imageCache[cardCode]?.let {
            println("Retrieving card image for $cardCode from cache")
            call.respond(it)
            return@get
        }

        val result = marvelCDbDataSource.getCardImage(cardCode)
        if (result.isFailure) {
            val throwable = result.exceptionOrNull()!!
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponseDto(HttpStatusCode.InternalServerError, throwable.message.toString())
            )
            return@get
        }

        imageCache[cardCode] = result.getOrThrow()
        call.respond(result.getOrThrow())

    }
}