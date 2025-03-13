package pro.schacher.mcc.server.plugins.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import pro.schacher.mcc.server.dto.CardDto
import pro.schacher.mcc.server.dto.PackDto
import pro.schacher.mcc.server.module
import pro.schacher.mcc.server.toDto
import kotlin.test.Test
import kotlin.test.assertTrue

class PacksRouteTest {
    @Test
    fun getAllPacksTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/packs")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getPackTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/packs/core")
        val pack = response.bodyAsText().toDto<PackDto>()
        assertEquals("core", pack.code)
    }

    @Test
    fun getCardsInPackTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/packs/core/cards")
        val cards = response.bodyAsText().toDto<List<CardDto>>()
        assertTrue(cards.isNotEmpty())
    }
}