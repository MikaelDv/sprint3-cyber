package br.com.challenge2026.challengeFord.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern SQL_META = Pattern.compile(
            "(?i)(\\bunion\\b|\\bselect\\b\\s+\\*|--\\s|/\\*|\\*/|\\bxp_|\\bsp_executesql\\b|;\\s*drop\\b|;\\s*delete\\b|;\\s*update\\b)"
    );
    private static final Pattern CMD_META = Pattern.compile("[;&|`$<>\\\\\\n\\r]");

    private InputSanitizer() {}

    public static String stripHtml(String input) {
        if (input == null) return null;
        String cleaned = Jsoup.clean(input, Safelist.none());
        return CONTROL_CHARS.matcher(cleaned).replaceAll("").trim();
    }

    public static String normalize(String input) {
        if (input == null) return null;
        String stripped = stripHtml(input);
        return stripped.replaceAll("\\s+", " ");
    }

    public static String sanitizeForPrompt(String input, int maxLength) {
        if (input == null) return "";
        String safe = normalize(input);
        if (safe.length() > maxLength) {
            safe = safe.substring(0, maxLength);
        }
        return safe.replace("\\", "").replace("\"", "'");
    }

    public static boolean containsSqlInjectionPattern(String input) {
        return input != null && SQL_META.matcher(input).find();
    }

    public static boolean containsCommandInjectionPattern(String input) {
        return input != null && CMD_META.matcher(input).find();
    }

    public static void rejectIfMalicious(String fieldName, String input) {
        if (input == null) return;
        if (containsSqlInjectionPattern(input) || containsCommandInjectionPattern(input)) {
            throw new IllegalArgumentException("Conteúdo inválido para o campo " + fieldName);
        }
    }
}
