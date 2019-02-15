package com.worldturner.medeia.api.jackson

import com.worldturner.medeia.api.ReaderSchemaSource
import org.junit.Test
import java.io.StringReader
import kotlin.test.assertNotNull

class KotlinJacksonApiTest {
    @Test
    fun testLoadStrangeSchema() {
        val medeia = MedeiaJacksonApi()
        val r = StringReader("{\"uniqueItems\":true}")
        val source = ReaderSchemaSource(r)
        val validator = medeia.loadSchemas(listOf(source))
        assertNotNull(validator)
    }
}