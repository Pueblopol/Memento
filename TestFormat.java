import java.util.regex.*;

public class TestFormat {
    public static void main(String[] args) {
        String oldText = "2. Due ";
        String newText = "2. Due\n";
        int cursor = newText.length();
        
        long newlinesInOld = oldText.chars().filter(ch -> ch == '\n').count();
        long newlinesInNew = newText.chars().filter(ch -> ch == '\n').count();
        
        boolean isNewlineInserted = cursor > 0 && newlinesInNew > newlinesInOld && newText.charAt(cursor - 1) == '\n';

        if (isNewlineInserted) {
            String textBeforeNewline = newText.substring(0, cursor - 1);
            int lastNewlineIdx = textBeforeNewline.lastIndexOf('\n');
            String previousLine = lastNewlineIdx == -1 ? textBeforeNewline : textBeforeNewline.substring(lastNewlineIdx + 1);
            System.out.println("previousLine: '" + previousLine + "'");
            
            Pattern numberRegex = Pattern.compile("^(\\d+)\\.\\s(.*)$");
            Matcher numMatch = numberRegex.matcher(previousLine);
            if (numMatch.find()) {
                System.out.println("num: " + numMatch.group(1));
            } else {
                System.out.println("No match!");
            }
        } else {
            System.out.println("isNewlineInserted is false");
        }
    }
}
