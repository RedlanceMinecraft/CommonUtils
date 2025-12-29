package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redlance.common.requester.boosty.BoostyRequester;
import org.redlance.common.requester.boosty.obj.user.BoostyUser;
import org.redlance.common.requester.boosty.obj.user.SubscribeStatus;

import java.io.IOException;
import java.util.List;

@EnabledIfSystemProperty(named = "boosty.token", matches = ".*")
public class BoostyRequesterTest {
    private static final String TOKEN = System.getProperty("boosty.token");

    @Test
    public void testEnum() throws IOException, InterruptedException {
        BoostyUser user = BoostyRequester.requestSubscribersChunking(List::getFirst, "dima_dencep", TOKEN, 2452741);
        Assertions.assertNotNull(user);
        Assertions.assertEquals(SubscribeStatus.ACTIVE, user.status);
    }
}
