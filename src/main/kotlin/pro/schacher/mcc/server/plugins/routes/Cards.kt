package pro.schacher.mcc.server.plugins.routes

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import pro.schacher.mcc.server.dto.CardsRequestDto
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.plugins.getAcceptLocaleOrDefault
import pro.schacher.mcc.server.plugins.getPathParameterOrThrow
import pro.schacher.mcc.server.plugins.runAndHandleErrors
import kotlin.collections.set

private val imageCache = mutableMapOf<String, ByteArray>()

private const val PREFIX = "/api/v1/cards"

val cardImageRateLimiter = RateLimitName("card_image")

internal fun Routing.cards(marvelCDbDataSource: MarvelCDbDataSource) {
    get(PREFIX) {
        runAndHandleErrors(call) {
            val local = call.getAcceptLocaleOrDefault()
            val cards = marvelCDbDataSource.getAllCards(local).getOrThrow()
            call.respond(cards)
        }
    }

    post(PREFIX) {
        runAndHandleErrors(call) {
            val local = call.getAcceptLocaleOrDefault()
            val cardsRequestDto = call.receive(CardsRequestDto::class)
            if (cardsRequestDto.cardCodes.isEmpty()) {
                throw BadRequestException("No card codes provided")
            }

            val cards = marvelCDbDataSource.getCards(local, cardsRequestDto.cardCodes).getOrThrow()
            call.respond(cards)
        }
    }

    get("$PREFIX/{cardCode}") {
        runAndHandleErrors(call) {
            val locale = call.getAcceptLocaleOrDefault()
            val cardCode = call.getPathParameterOrThrow("cardCode")
            val card = marvelCDbDataSource.getCard(locale, cardCode).getOrThrow()

            call.respond(card)
        }
    }

    rateLimit(cardImageRateLimiter) {
        get("$PREFIX/{cardCode}/image") {
            runAndHandleErrors(call) {
                val cardCode = call.getPathParameterOrThrow("cardCode")

                imageCache[cardCode]?.let {
                    println("Retrieving card image for $cardCode from cache")
                    call.respond(it)
                    return@runAndHandleErrors
                }

                val image = marvelCDbDataSource.getCardImage(cardCode).getOrThrow()
                imageCache[cardCode] = image
                call.respond(image)
            }
        }
    }
}