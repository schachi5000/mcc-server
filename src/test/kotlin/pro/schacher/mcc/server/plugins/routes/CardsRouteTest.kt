package pro.schacher.mcc.server.plugins.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import pro.schacher.mcc.server.dto.CardDto
import pro.schacher.mcc.server.module
import pro.schacher.mcc.server.toDto
import kotlin.test.Test

class CardsRouteTest {
    @Test
    fun getAllCardsTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/cards")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun getCardTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/cards/01001a")
        val dto = response.bodyAsText().toDto<CardDto>()
        assertEquals("Spider-Man", dto.name)
    }

    @Test
    fun getCardImageTest() = testApplication {
        application {
            module()
        }

        val response = this.client.get("/api/v1/cards/01001a/image")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}