import java.util.regex.*;
public class TestRegex3 {
    public static void main(String[] args) {
        String test = "2. Due";
        Matcher m = Pattern.compile("^(\\d+)\\.([\\s\\u00A0]+)(.*)$").matcher(test);
        if (m.find()) {
            System.out.println("G1: " + m.group(1));
            System.out.println("G2: " + m.group(2));
            System.out.println("G3: " + m.group(3));
        }
    }
}
