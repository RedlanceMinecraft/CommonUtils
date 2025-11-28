package org.redlance.common.tests;

import com.github.mizosoft.methanol.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.utils.ServerUtils;

public class ServerUtilsTest {
    @Test
    public void test() {
        String header = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
        Assertions.assertTrue(ServerUtils.isTypeAccepted(header, MediaType.TEXT_HTML));
        Assertions.assertFalse(ServerUtils.isTypeAccepted(header, MediaType.APPLICATION_JSON));
    }
}
