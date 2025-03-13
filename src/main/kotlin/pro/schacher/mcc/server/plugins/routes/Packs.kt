package pro.schacher.mcc.server.plugins.routes

import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import pro.schacher.mcc.server.marvelcdb.MarvelCDbDataSource
import pro.schacher.mcc.server.plugins.getAcceptLocaleOrDefault
import pro.schacher.mcc.server.plugins.getPathParameterOrThrow
import pro.schacher.mcc.server.plugins.runAndHandleErrors

private const val PREFIX = "/api/v1/packs"

internal fun Routing.packs(marvelCDbDataSource: MarvelCDbDataSource) {
    get(PREFIX) {
        runAndHandleErrors(call) {
            val allPacks = marvelCDbDataSource.getAllPacks().getOrThrow()
            call.respond(allPacks)
        }
    }

    get("$PREFIX/{packCode?}") {
        runAndHandleErrors(call) {
            val packCode = call.getPathParameterOrThrow("packCode")
            val pack = marvelCDbDataSource.getAllPacks().getOrNull()?.find {
                it.code == packCode
            }

            if (pack == null) {
                throw NotFoundException("Pack with code $packCode not found")
            }

            call.respond(pack)
        }
    }

    get("$PREFIX/{packCode?}/cards") {
        runAndHandleErrors(call) {
            val local = call.getAcceptLocaleOrDefault()
            val packCode = call.getPathParameterOrThrow("packCode")
            val allPacks = marvelCDbDataSource.getCardsInPack(local, packCode).getOrThrow()
            call.respond(allPacks)
        }
    }
}