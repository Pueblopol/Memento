import java.io.File
import java.util.UUID

val repoDir = File("test_find_repo")
repoDir.mkdirs()
val note1 = File(repoDir, "Note1.md")
val id = UUID.randomUUID().toString()
note1.writeText("""
---
id: "$id"
folders: ["OldFolder"]
---
Hello
""".trimIndent())

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
println("Found ID: $foundId")
