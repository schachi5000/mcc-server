package pro.schacher.mcc.server.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource
import pro.schacher.mcc.server.plugins.routes.*

fun Application.configureRouting(marvelCDbDataSource: MarvelCDbDataSource) {
    routing {
        card(marvelCDbDataSource)
        deck(marvelCDbDataSource)
        decks(marvelCDbDataSource)
        pack(marvelCDbDataSource)
        packs(marvelCDbDataSource)
        spotlight(marvelCDbDataSource)
        health()
    }
}





