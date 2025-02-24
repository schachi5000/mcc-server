package pro.schacher.mcc.server.plugins.routes

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import pro.schacher.mcc.server.module
import kotlin.test.Test

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
        assertEquals(HttpStatusCode.OK, response.status)
    }
}