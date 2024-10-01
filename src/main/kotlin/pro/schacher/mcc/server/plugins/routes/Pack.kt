package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

fun Routing.pack(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/pack/{packCode?}") {
        val packCode = call.pathParameters["packCode"]
        if (packCode == null) {
            call.respond(HttpStatusCode.BadRequest, "No pack found")
            return@get
        }

        val allPacks = marvelCDbDataSource.getCardsInPack(packCode)
        call.respond(allPacks)
    }
}
