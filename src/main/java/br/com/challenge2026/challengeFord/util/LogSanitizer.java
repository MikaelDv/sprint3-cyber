package br.com.challenge2026.challengeFord.util;

import java.util.regex.Pattern;

public final class LogSanitizer {

    private static final Pattern CRLF = Pattern.compile("[\\r\\n]");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CREDIT_CARD = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-+/=]+");

    private LogSanitizer() {}

    public static String safe(String value) {
        if (value == null) return null;
        String result = CRLF.matcher(value).replaceAll("_");
        result = EMAIL.matcher(result).replaceAll("[email-redacted]");
        result = CPF.matcher(result).replaceAll("[cpf-redacted]");
        result = CREDIT_CARD.matcher(result).replaceAll("[card-redacted]");
        result = BEARER.matcher(result).replaceAll("Bearer [token-redacted]");
        if (result.length() > 500) {
            result = result.substring(0, 500) + "...[truncated]";
        }
        return result;
    }

    public static String mask(String value, int visible) {
        if (value == null || value.length() <= visible) return "***";
        return value.substring(0, visible) + "***";
    }
}
