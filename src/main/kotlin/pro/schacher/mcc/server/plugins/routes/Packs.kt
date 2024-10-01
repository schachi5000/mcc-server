package pro.schacher.mcc.server.plugins.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

internal fun Routing.packs(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/packs") {
        val allPacks = marvelCDbDataSource.getAllPacks()
        call.respond(allPacks)
    }
}
