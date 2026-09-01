package com.pol.memento

import org.junit.Test
import java.io.File
import java.util.UUID
import org.junit.Assert.assertTrue

class FindFileTest {
    @Test
    fun testFindFile() {
        val repoDir = File("build/test_find_repo")
        repoDir.mkdirs()
        val note1 = File(repoDir, "Note1.md")
        val id = UUID.randomUUID().toString()
        note1.writeText("""---
id: "$id"
folders: ["OldFolder"]
---
Hello""")

        var foundId = false
        val reader = note1.bufferedReader()
        var line = reader.readLine()
        if (line?.trim() == "---") {
            for (i in 0..10) {
                line = reader.readLine() ?: break
                if (line.trim() == "---") break
                if (line.startsWith("id:") && line.substringAfter(":").trim().removeSurrounding("\"") == id) {
                    foundId = true
                    break
                }
            }
        }
        reader.close()
        System.out.println("FOUND ID: " + foundId)
        assertTrue(foundId)
    }
}
