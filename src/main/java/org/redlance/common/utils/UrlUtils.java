package org.redlance.common.utils;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class UrlUtils {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:[a-z0-9]{2,}:\\/\\/)?(?>(?:[0-9]{1,3}\\.){3}[0-9]{1,3}" +
                    "|(?:[-\\w_]+\\.)+(?:com|net|org|jp|de|uk|fr|br|it|ru|es|me|gov|pl|ca|au|cn|co|in|nl|se|no|fi|mx|kr|ch|ua|vn|cz|gr|at|be|ar|dk|hk|pt|nz|za|sg|my|th|tw|gg|ms|online|edu|mil|io|info|biz|us))" +
                    "(?::[0-9]{1,5})?[^!\"\\u00A7 \\n]*",
            Pattern.CASE_INSENSITIVE
    );

    public static Matcher matcher(String input) {
        return URL_PATTERN.matcher(input);
    }

    public static String filterUrls(@Nullable String input, String replace) {
        if (input == null) return null;

        String replacement = (replace != null) ? replace : "";
        return URL_PATTERN.matcher(input).replaceAll(replacement);
    }
}
