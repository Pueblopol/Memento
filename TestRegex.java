import java.util.regex.*;

public class TestRegex {
    public static void main(String[] args) {
        String previousLine = "☐ Task 1";
        Pattern checkboxRegex = Pattern.compile("^(☐|☑)\\s(.*)$");
        Matcher checkMatch = checkboxRegex.matcher(previousLine);
        if (checkMatch.find()) {
            System.out.println("checkMatch found!");
        } else {
            System.out.println("No match!");
        }
    }
}
