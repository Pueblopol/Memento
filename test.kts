import java.util.UUID

data class ParsedNoteData(val id: String, val title: String, val description: String, val needsRewrite: Boolean)

fun deserialize(content: String, defaultTitle: String): ParsedNoteData? {
    val lines = content.lines()
    if (lines.isEmpty()) return null
    var id = UUID.randomUUID().toString()
    var title = defaultTitle
    var needsRewrite = false
    var i = 0
    var hasYaml = false
    if (lines[i].trim() == "---") {
        hasYaml = true
        // ...
    } else {
        needsRewrite = true
    }
    
    val descriptionBuilder = StringBuilder()
    var firstHeadingSkipped = false
    while (i < lines.size) {
        val line = lines[i]
        if (hasYaml && !firstHeadingSkipped && line.startsWith("# ") && line.substringAfter("# ").trim() == title) {
            firstHeadingSkipped = true
            i++
            if (i < lines.size && lines[i].isBlank()) i++
            continue
        }
        descriptionBuilder.appendLine(line)
        i++
    }
    return ParsedNoteData(id, title, descriptionBuilder.toString().trimEnd(), needsRewrite)
}

println(deserialize("# Hello World\nThis is a test note from PC.", "Hello World"))
