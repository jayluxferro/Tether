package com.koushikdutta.async.http.libcore;

final class HeaderParser {

    public interface CacheControlHandler {
        void handle(String str, String str2);
    }

    HeaderParser() {
    }

    public static void parseCacheControl(String value, CacheControlHandler handler) {
        int pos = 0;
        while (pos < value.length()) {
            int tokenStart = pos;
            pos = skipUntil(value, pos, "=,");
            String directive = value.substring(tokenStart, pos).trim();
            if (pos == value.length() || value.charAt(pos) == ',') {
                pos++;
                handler.handle(directive, null);
            } else {
                String parameter;
                pos = skipWhitespace(value, pos + 1);
                int parameterStart;
                if (pos >= value.length() || value.charAt(pos) != '\"') {
                    parameterStart = pos;
                    pos = skipUntil(value, pos, ",");
                    parameter = value.substring(parameterStart, pos).trim();
                } else {
                    pos++;
                    parameterStart = pos;
                    pos = skipUntil(value, pos, "\"");
                    parameter = value.substring(parameterStart, pos);
                    pos++;
                }
                handler.handle(directive, parameter);
            }
        }
    }

    private static int skipUntil(String input, int pos, String characters) {
        while (pos < input.length() && characters.indexOf(input.charAt(pos)) == -1) {
            pos++;
        }
        return pos;
    }

    private static int skipWhitespace(String input, int pos) {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c != ' ' && c != 9) {
                break;
            }
            pos++;
        }
        return pos;
    }

    public static int parseSeconds(String value) {
        try {
            long seconds = Long.parseLong(value);
            if (seconds > 2147483647L) {
                return Integer.MAX_VALUE;
            }
            if (seconds < 0) {
                return 0;
            }
            return (int) seconds;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
