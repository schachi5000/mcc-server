package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

internal fun Routing.spotlight(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/spotlight/{date?}") {
        val date = call.pathParameters["date"]
        if (date == null) {
            call.respond(HttpStatusCode.BadRequest, "No pack found")
            return@get
        }

        val allPacks = marvelCDbDataSource.getSpotlightDecksByDate(date)
        call.respond(allPacks)
    }
}