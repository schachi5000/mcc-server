package pro.schacher.mcc.server.plugins.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import pro.schacher.mcc.server.dto.DeckDto
import pro.schacher.mcc.server.module
import pro.schacher.mcc.server.toDto
import kotlin.test.Test

class DecksRouteTest {
    @Test
    fun getSpotlightDecks() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/decks/spotlight?date=2025-02-26")
        val decks = response.bodyAsText().toDto<List<DeckDto>>()
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(decks.isNotEmpty())
    }
}