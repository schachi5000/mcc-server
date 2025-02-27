package pro.schacher.mcc.server.plugins.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import pro.schacher.mcc.server.dto.CardDto
import pro.schacher.mcc.server.dto.CardsRequestDto
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
    fun getCardsTest() = testApplication {
        application {
            module()
        }

        val cards = this.client.post("/api/v1/cards") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(CardsRequestDto(listOf("01001a", "01001b"))))
        }
            .bodyAsText()
            .toDto<List<CardDto>>()

        assertEquals(2, cards.size)
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