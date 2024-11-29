package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource

private const val PREFIX = "/api/v1/packs"

internal fun Routing.packs(marvelCDbDataSource: MarvelCDbDataSource) {
    get(PREFIX) {
        val allPacks = marvelCDbDataSource.getAllPacks()
        call.respond(allPacks)
    }

    get("$PREFIX/{packCode?}") {
        val packCode = call.pathParameters["packCode"]
        if (packCode == null) {
            call.respond(HttpStatusCode.BadRequest, "No pack found")
            return@get
        }

        val allPacks = marvelCDbDataSource.getCardsInPack(packCode)
        call.respond(allPacks)
    }
}

