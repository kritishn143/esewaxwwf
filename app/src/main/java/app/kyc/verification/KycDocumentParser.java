package app.kyc.verification;

import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-grade spatial parser that uses ML Kit's bounding boxes
 * to find values based on their physical location relative to labels,
 * rather than relying on unpredictable flat text merging.
 */
public class KycDocumentParser {

    public static class KycData {
        public String nin = "";
        public String fullName = "";
        public String dob = "";
        public String sex = "";
        public String nationality = "";
        public String address = "";
    }

    public static KycData parseFront(Text visionText) {
        KycData data = new KycData();
        List<Text.Line> lines = getAllLines(visionText);
        String fullText = visionText.getText();

        // 1. NIN: Very strict format, regex is safe anywhere
        data.nin = extractRegex(fullText, "(\\d{3}-\\d{3}-\\d{4})");

        // 2. DOB: Always extract the earliest date in the document.
        // Because the Nepali year (BS) is ~57 years ahead of the English year (AD),
        // the smallest year found on the card is mathematically guaranteed to be the English AD date.
        data.dob = getEarliestDate(fullText);

        // 3. Name: Find the label, then get the English text directly below it
        Text.Line nameLabel = findLineMatching(lines, "(?i)full\\s*name|\\bname\\b");
        if (nameLabel != null) {
            List<Text.Line> linesBelow = findLinesBelow(lines, nameLabel, 3);
            for (Text.Line line : linesBelow) {
                String text = line.getText().trim();
                // Skip Nepali lines. Ensure it has at least one purely alphabetical English word of 3+ letters.
                if (text.matches(".*[a-zA-Z]+.*") && text.matches(".*\\b[A-Za-z]{3,}\\b.*")) {
                    // Ensure it's not the next label
                    if (!text.matches("(?i).*\\b(dob|date|birth|nin|sex|gender|nationality)\\b.*")) {
                        data.fullName = text.replaceAll("[^a-zA-Z\\s]", "").trim();
                        if (!data.fullName.isEmpty()) break;
                    }
                }
            }
        }
        
        // Fallback for inline Name
        if (data.fullName == null || data.fullName.isEmpty()) {
            String inlineName = extractRegex(fullText, "(?i)\\bname[:\\s]+([A-Z][a-z]+\\s+[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)");
            if (inlineName != null) data.fullName = inlineName;
        }

        // Clean up name (remove non-alpha)
        if (data.fullName != null) {
            data.fullName = data.fullName.replaceAll("[^a-zA-Z\\s]", "").trim();
        }

        // 4. Sex: Spatial or inline
        Text.Line sexLabel = findLineMatching(lines, "(?i)^\\s*sex\\s*:?\\s*$");
        if (sexLabel != null) {
            Text.Line sexVal = findNearestLineBelowOrRight(lines, sexLabel);
            if (sexVal != null) data.sex = parseSex(sexVal.getText());
        }
        if (data.sex == null || data.sex.isEmpty()) {
            data.sex = parseSex(extractRegex(fullText, "(?i)\\bsex\\b[^a-zA-Z]*(male|female|M|F)\\b"));
        }

        // 5. Nationality
        Text.Line natLabel = findLineMatching(lines, "(?i)nationality");
        if (natLabel != null) {
            Text.Line natVal = findNearestLineBelowOrRight(lines, natLabel);
            if (natVal != null) data.nationality = extractRegex(natVal.getText(), "([A-Za-z]+)");
        }
        if (data.nationality == null || data.nationality.isEmpty()) {
            data.nationality = extractRegex(fullText, "(?i)\\bnationality\\b[^a-zA-Z]*([A-Za-z]+)");
        }
        // Ultimate fallback for Nepal IDs
        if ((data.nationality == null || data.nationality.isEmpty()) && 
            (fullText.toLowerCase().contains("nepali") || fullText.toLowerCase().contains("nepal"))) {
            data.nationality = "Nepali";
        }

        return data;
    }

    public static KycData parseBack(Text visionText) {
        KycData data = new KycData();
        List<Text.Line> lines = getAllLines(visionText);

        // 1. Find the Permanent Address label
        Text.Line addressLabel = findLineMatching(lines, "(?i)permanent\\s*address|address");
        if (addressLabel != null) {
            // Get up to 4 lines below the label
            List<Text.Line> linesBelow = findLinesBelow(lines, addressLabel, 4);
            List<String> validEnglishLines = new ArrayList<>();
            
            for (Text.Line line : linesBelow) {
                String text = line.getText().trim();
                
                // We only want lines that are actual English addresses.
                // Hallucinated Nepali text often lacks full English words.
                // We check if the line contains at least one purely alphabetical word of 3+ letters.
                if (text.matches(".*[a-zA-Z]+.*") && isLikelyAddressPart(text)) {
                    // Check for a solid English word to confirm it's not a hallucination
                    if (text.matches(".*\\b[A-Za-z]{3,}\\b.*")) {
                        validEnglishLines.add(text);
                    }
                }
            }
            
            if (!validEnglishLines.isEmpty()) {
                // If we found multiple valid lines, join them
                data.address = String.join(", ", validEnglishLines);
            }
        }
        
        // Fallback: Just grab ANY line containing common address keywords
        if (data.address == null || data.address.isEmpty() || data.address.length() < 5) {
            Text.Line fallbackLine = findLineMatching(lines, "(?i)Municipality|Metropolitan|City|Rural|Ward|District|Tole|Marg");
            if (fallbackLine != null) data.address = fallbackLine.getText().trim();
        }
        
        // Final fallback: Use regex to find anything that looks like an address (e.g., words followed by number, comma, word)
        if (data.address == null || data.address.isEmpty() || data.address.length() < 5) {
             String regexAddress = extractRegex(visionText.getText(), "([A-Z][a-zA-Z\\s]+(?:Municipality|Metropolitan|City|Rural|Ward|District|Tole|Marg)?\\s*[-]?\\s*\\d+,?\\s*[A-Z][a-zA-Z]+)");
             if (regexAddress != null) data.address = regexAddress;
        }

        // Clean up
        if (data.address != null) {
            data.address = data.address.replaceAll("(?i)^.*(permanent|address)\\s*", "");
            
            // Fix common ML Kit OCR errors on Nepali fonts
            data.address = data.address.replaceAll("(?i)3hapa", "Jhapa");
            // Fix missing spaces after commas
            data.address = data.address.replaceAll(",([a-zA-Z])", ", $1");
            // Remove any trailing commas
            data.address = data.address.replaceAll(",\\s*$", "");
        }

        return data;
    }

    // --- Spatial Helpers ---

    private static List<Text.Line> getAllLines(Text visionText) {
        List<Text.Line> allLines = new ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            allLines.addAll(block.getLines());
        }
        return allLines;
    }

    private static Text.Line findLineMatching(List<Text.Line> lines, String regex) {
        Pattern p = Pattern.compile(regex);
        for (Text.Line line : lines) {
            if (p.matcher(line.getText()).find()) {
                return line;
            }
        }
        return null;
    }

    private static List<Text.Line> findLinesBelow(List<Text.Line> lines, Text.Line target, int limit) {
        Rect targetBox = target.getBoundingBox();
        if (targetBox == null) return new ArrayList<>();

        List<Text.Line> belowLines = new ArrayList<>();
        for (Text.Line line : lines) {
            if (line == target) continue;
            Rect box = line.getBoundingBox();
            if (box == null) continue;

            // Line must be vertically below the target
            if (box.top >= targetBox.bottom - 15) {
                // Ensure horizontal overlap/proximity to prevent grabbing unrelated far-right/left text
                if (box.right > targetBox.left - 50 && box.left < targetBox.right + 200) {
                    belowLines.add(line);
                }
            }
        }

        // Sort lines top-to-bottom
        Collections.sort(belowLines, (l1, l2) -> {
            Rect b1 = l1.getBoundingBox();
            Rect b2 = l2.getBoundingBox();
            if (b1 == null || b2 == null) return 0;
            return Integer.compare(b1.top, b2.top);
        });

        if (belowLines.size() > limit) {
            return belowLines.subList(0, limit);
        }
        return belowLines;
    }

    private static Text.Line findNearestLineBelow(List<Text.Line> lines, Text.Line target) {
        List<Text.Line> below = findLinesBelow(lines, target, 1);
        return below.isEmpty() ? null : below.get(0);
    }

    private static Text.Line findNearestLineBelowOrRight(List<Text.Line> lines, Text.Line target) {
        Rect targetBox = target.getBoundingBox();
        if (targetBox == null) return null;

        Text.Line bestLine = null;
        int minDistance = Integer.MAX_VALUE;

        for (Text.Line line : lines) {
            if (line == target) continue;
            Rect box = line.getBoundingBox();
            if (box == null) continue;

            // Below or right
            boolean isBelow = (box.top >= targetBox.bottom - 10 && box.centerX() > targetBox.left - 50 && box.centerX() < targetBox.right + 50);
            boolean isRight = (box.left >= targetBox.right - 10 && box.centerY() > targetBox.top - 20 && box.centerY() < targetBox.bottom + 20);

            if (isBelow || isRight) {
                int dx = box.centerX() - targetBox.centerX();
                int dy = box.centerY() - targetBox.centerY();
                int distance = (int) Math.sqrt(dx * dx + dy * dy);
                if (distance < minDistance && distance < 300) {
                    minDistance = distance;
                    bestLine = line;
                }
            }
        }
        return bestLine;
    }

    // --- Data Extractors ---

    private static String extractRegex(String text, String regex) {
        if (text == null) return null;
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String parseSex(String raw) {
        if (raw == null) return null;
        raw = raw.trim().toUpperCase();
        if (raw.startsWith("M")) return "Male";
        if (raw.startsWith("F")) return "Female";
        return null;
    }

    private static String getEarliestDate(String text) {
        Matcher m = Pattern.compile("(\\d{4})[-/](\\d{2})[-/](\\d{2})").matcher(text);
        int earliest = 9999;
        String bestDate = null;
        while (m.find()) {
            int y = Integer.parseInt(m.group(1));
            if (y < earliest) {
                earliest = y;
                bestDate = m.group(1) + "-" + m.group(2) + "-" + m.group(3);
            }
        }
        return bestDate;
    }

    private static boolean isLikelyAddressPart(String text) {
        return Pattern.compile("(?i)[a-z]+").matcher(text).find() && !text.matches("(?i).*\\b(issue|date|signature|expire|citizenship|type|nin|cc|number|officer)\\b.*");
    }
}
