package org.redlance.common.utils;

import org.redlance.common.CommonUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    private static final Pattern URL_PATTERN = Pattern.compile(
            //         schema                          ipv4            OR        namespace                 port     path         ends
            //   |-----------------|        |-------------------------|  |-------------------------|    |---------| |--|   |---------------|
            "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"\u00A7 \n]|$))",
            Pattern.CASE_INSENSITIVE
    );

    public static Matcher matcher(String input) {
        return URL_PATTERN.matcher(input);
    }

    public static String filterUrls(String input, String replace) {
        Matcher matcher = matcher(input);

        while (matcher.find()) {
            String url = input.substring(matcher.start(), matcher.end());

            CommonUtils.LOGGER.trace("Found url {} in {}.", url, input);
            input = input.replace(url, replace);
        }

        return input;
    }
}
