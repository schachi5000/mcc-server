package pro.schacher.mcc.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.application.Application
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
    } catch (e: Exception) {
        call.respond(
            InternalServerError,
            ErrorResponseDto(InternalServerError.description, e.message)
        )
    }
}

fun RoutingCall.getPathParameterOrThrow(parameter: String) = this.pathParameters[parameter]
    ?: throw RemoteServiceException(HttpStatusCode.BadRequest, "Parameter [$parameter] missing")

fun RoutingCall.getQueryParameterOrThrow(parameter: String) =
    this.request.queryParameters[parameter]
        ?: throw RemoteServiceException(HttpStatusCode.BadRequest, "Parameter [$parameter] missing")




