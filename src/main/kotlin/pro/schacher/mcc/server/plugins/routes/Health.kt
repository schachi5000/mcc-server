package pro.schacher.mcc.server.plugins.routes

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Routing.health() {
    get("/health") {
        call.respond(HttpStatusCode.OK)
    }
}