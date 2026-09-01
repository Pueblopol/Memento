public class TestRegex2 {
    public static void main(String[] args) {
        String test = "2. ";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\.\\s(.*)$").matcher(test);
        if (m.find()) {
            System.out.println("Empty string match: " + m.group(2).isEmpty());
        }
    }
}
