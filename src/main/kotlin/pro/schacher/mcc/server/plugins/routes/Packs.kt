package pro.schacher.mcc.server.plugins.routes

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import pro.schacher.mcc.server.datasource.MarvelCDbDataSource

internal fun Routing.packs(marvelCDbDataSource: MarvelCDbDataSource) {
    get("/packs") {
        val allPacks = marvelCDbDataSource.getAllPacks()
        println(allPacks.joinToString())
        call.respond(Test("test"))
    }
}

@Serializable
data class Test(
    val name: String,
)


