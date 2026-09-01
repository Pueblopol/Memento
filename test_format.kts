val newText = "1. Uno\n2. Due\n"
val oldText = "1. Uno\n2. Due"
val cursor = newText.length
val isNewlineInserted = cursor > 0 && newText.length > oldText.length && newText[cursor - 1] == '\n'

if (isNewlineInserted) {
    val textBeforeNewline = newText.substring(0, cursor - 1)
    val previousLine = textBeforeNewline.substringAfterLast('\n')
    println("previousLine: '$previousLine'")
    
    val numberRegex = Regex("^(\\d+)\\.\\s(.*)$")
    val numMatch = numberRegex.find(previousLine)
    if (numMatch != null) {
        val num = numMatch.groupValues[1].toInt()
        val content = numMatch.groupValues[2]
        println("num: $num, content: '$content'")
        
        val prefix = "${num + 1}. "
        val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
        println("newStr: '$newStr'")
    } else {
        println("No match!")
    }
}
