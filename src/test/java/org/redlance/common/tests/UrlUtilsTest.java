package org.redlance.common.tests;

import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.utils.UrlUtils;

import java.util.concurrent.TimeUnit;

public class UrlUtilsTest {
    @Test
    public void testSingle() {
        String text = "visit https://google.com";
        String filtered = UrlUtils.filterUrls(text, "REDACTED");
        Assertions.assertEquals("visit REDACTED", filtered);
    }

    @Test
    public void testMulti() {
        String text = "visit https://google.com and https://github.com";
        String filtered = UrlUtils.filterUrls(text, "REDACTED");
        Assertions.assertEquals("visit REDACTED and REDACTED", filtered);
    }

    @Test
    public void bench() {
        StringBuilder text = new StringBuilder();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            text.append("https://google.com\n");
            result.append("REDACTED\n");
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        String filtered = UrlUtils.filterUrls(text.toString(), "REDACTED");

        long took = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        System.out.println("took " + took + "ms");

        Assertions.assertEquals(result.toString(), filtered);
        Assertions.assertTrue(took < 30);
    }
}
