val nbsp = "\u00A0"
val text = "2." + nbsp + "Due"
val regex = Regex("^(\\d+)\\.\\s(.*)$")
val match = regex.find(text)
if (match != null) {
    println("Matched!")
} else {
    println("No match!")
}
