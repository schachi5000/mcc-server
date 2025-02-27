package pro.schacher.mcc.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.routing
import pro.schacher.mcc.server.dto.ErrorResponseDto
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.marvelcdb.RemoteServiceException
import pro.schacher.mcc.server.plugins.routes.cards
import pro.schacher.mcc.server.plugins.routes.decks
import pro.schacher.mcc.server.plugins.routes.health
import pro.schacher.mcc.server.plugins.routes.packs
import java.util.Locale

fun Application.configureRouting(marvelCDbDataSource: MarvelCDbDataSource) {
    routing {
        cards(marvelCDbDataSource)
        decks(marvelCDbDataSource)
        packs(marvelCDbDataSource)
        health()
    }
}

suspend fun runAndHandleErrors(call: RoutingCall, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: RemoteServiceException) {
        call.respond(e.statusCode, ErrorResponseDto(e.error, e.message))
    } catch (e: BadRequestException) {
        call.respond(BadRequest, ErrorResponseDto(BadRequest.description, e.message))
    } catch (e: Exception) {
        call.respond(InternalServerError, ErrorResponseDto(e.toString(), e.message))
    }
}

fun RoutingCall.getAcceptLocaleOrDefault(): Locale = this.request.headers["Accept-Language"].let {
    when {
        it == null -> Locale.ENGLISH
        it.contains("de", ignoreCase = true) -> Locale.GERMAN
        else -> Locale.ENGLISH
    }
}

fun RoutingCall.getPathParameterOrThrow(parameter: String) = this.pathParameters[parameter]
    ?: throw RemoteServiceException(HttpStatusCode.BadRequest, "Parameter [$parameter] missing")

fun RoutingCall.getQueryParameterOrThrow(parameter: String) =
    this.request.queryParameters[parameter]
        ?: throw RemoteServiceException(HttpStatusCode.BadRequest, "Parameter [$parameter] missing")




