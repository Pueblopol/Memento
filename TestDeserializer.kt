// Test Logic
import java.util.UUID

fun main() {
    val content = """---
id: "123"
title: "Test"
---

# Test

This is the first line."""
    
    val lines = content.lines()
    var i = 0
    while (i < lines.size && lines[i].trim() != "---") i++
    i++ // Skip first ---
    while (i < lines.size && lines[i].trim() != "---") i++
    i++ // Skip second ---
    
    if (i < lines.size && lines[i].isBlank()) i++
    
    val descriptionBuilder = StringBuilder()
    var firstHeadingSkipped = false
    while (i < lines.size) {
        val line = lines[i]
        if (!firstHeadingSkipped && line.startsWith("# ") && line.substringAfter("# ").trim() == "Test") {
            firstHeadingSkipped = true
            i++
            if (i < lines.size && lines[i].isBlank()) i++
            continue
        }
        descriptionBuilder.appendLine(line)
        i++
    }
    
    println("Description:")
    println(descriptionBuilder.toString().trimEnd())
}
