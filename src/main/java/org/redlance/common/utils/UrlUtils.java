package org.redlance.common.utils;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class UrlUtils {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}" +
                    "|(?:[-\\w_]+\\.(?:gg|ms|com|me|net|org|online|edu|gov|mil|io|info|biz|co|ru|uk|de|jp|fr|au|us|ca|cn|in|es|br|it|nl|se|no|fi|mx|kr|ch|ua|pl|vn|cz|gr|at|be|ar|dk|hk|pt|nz|za|sg|my|th|tw)))" +
                    "(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE
    );

    public static Matcher matcher(String input) {
        return URL_PATTERN.matcher(input);
    }

    public static String filterUrls(@Nullable String input, String replace) {
        if (input == null) return null;

        Matcher matcher = matcher(input);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(input, lastEnd, matcher.start());
            if (replace != null) result.append(replace);
            lastEnd = matcher.end();
        }

        result.append(input.substring(lastEnd));
        return result.toString();
    }
}
