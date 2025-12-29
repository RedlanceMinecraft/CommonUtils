package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.utils.UrlUtils;

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
    public void testSub() {
        String text = "visit https://co.google.com and https://ed.github.com";
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

        long time = System.currentTimeMillis();
        String filtered = UrlUtils.filterUrls(text.toString(), "REDACTED");

        long took = System.currentTimeMillis() - time;
        System.out.println("took " + took + "ms");

        Assertions.assertEquals(result.toString(), filtered);
        // Assertions.assertTrue(took < 30);
    }
}
