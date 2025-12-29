package org.redlance.common.tests;

import com.github.mizosoft.methanol.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.requester.RequesterUtils;

public class ServerUtilsTest {
    @Test
    public void test() {
        String header = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
        Assertions.assertTrue(RequesterUtils.isTypeAccepted(header, MediaType.TEXT_HTML));
        Assertions.assertFalse(RequesterUtils.isTypeAccepted(header, MediaType.APPLICATION_JSON));
    }
}
