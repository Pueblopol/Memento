package com.pol.memento

import org.junit.Test
import com.pol.memento.sync.MarkdownDeserializer

class DeserializerTest {
    @Test
    fun testPlain() {
        val md = """
            # Hello World
            This is a test note from PC.
        """.trimIndent()
        val parsed = MarkdownDeserializer.deserialize(md, "Hello World")
        throw Exception("RESULT_NOTE_DESC: '${parsed?.note?.description}'")
    }
}
