package mar.utils;

import java.net.URL;

public class StringProcessor {
    public static String replaceFirstOccurrence(String original, String find, String replace) {
        int index = original.indexOf(find);
        if (index != -1) {
            return original.substring(0, index) + replace + original.substring(index + find.length());
        }
        return original;
    }

    public static boolean matchFromEnd(String input, String pattern) {
        int inputLength = input.length();
        int patternLength = pattern.length();

        int inputIndex = inputLength - 1;
        int patternIndex = patternLength - 1;

        while (inputIndex >= 0 && patternIndex >= 0) {
            if (input.charAt(inputIndex) != pattern.charAt(patternIndex)) {
                return false;
            }
            inputIndex--;
            patternIndex--;
        }

        // 如果patternIndex为-1，表示pattern字符串已经完全匹配
        return patternIndex == -1;
    }

    public static String getHostByUrl(String url) {
        String host = "";

        try {
            URL u = new URL(url);
            int port = u.getPort();
            if (port == -1) {
                host = u.getHost();
            } else {
                host = String.format("%s:%s", u.getHost(), port);
            }
        } catch (Exception ignored) {
        }

        return host;
    }
}