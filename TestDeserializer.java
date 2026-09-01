public class TestDeserializer {
    public static void main(String[] args) {
        String content = "---\nid: \"123\"\ntitle: \"Test\"\n---\n\n# Test\n\nThis is the first line.";
        String[] lines = content.split("\n", -1);
        int i = 0;
        while (i < lines.length && !lines[i].trim().equals("---")) i++;
        i++;
        while (i < lines.length && !lines[i].trim().equals("---")) i++;
        i++;
        
        if (i < lines.length && lines[i].trim().isEmpty()) i++;
        
        StringBuilder descriptionBuilder = new StringBuilder();
        boolean firstHeadingSkipped = false;
        while (i < lines.length) {
            String line = lines[i];
            if (!firstHeadingSkipped && line.startsWith("# ") && line.substring(2).trim().equals("Test")) {
                firstHeadingSkipped = true;
                i++;
                if (i < lines.length && lines[i].trim().isEmpty()) i++;
                continue;
            }
            descriptionBuilder.append(line).append("\n");
            i++;
        }
        
        System.out.println("Description:\n'" + descriptionBuilder.toString().trim() + "'");
    }
}
