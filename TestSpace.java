import java.util.regex.*;

public class TestSpace {
    public static void main(String[] args) {
        String nbsp = "\u00A0";
        String text = "2." + nbsp + "Due";
        Pattern regex = Pattern.compile("^(\\d+)\\.\\s(.*)$");
        Matcher match = regex.matcher(text);
        if (match.find()) {
            System.out.println("Matched!");
        } else {
            System.out.println("No match!");
        }
    }
}
