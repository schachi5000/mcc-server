package pro.schacher.mcc.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
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





