package org.redlance.common.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redlance.common.emotecraft.utils.UuidCodec;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class UuidCodecTest {
    @Test
    void roundTrip() {
        for (int len = 1; len <= 26; len++) {
            String name = randomName(len);
            UUID uuid = UuidCodec.encodeName(name);
            String decoded = UuidCodec.decodeName(uuid);
            Assertions.assertEquals(name, decoded, "Failed at length " + len + ": " + name);
        }
    }

    @Test
    void emptyReturnsEmpty() {
        UUID uuid = UuidCodec.encodeName("");
        Assertions.assertEquals("", UuidCodec.decodeName(uuid));
    }

    private static String randomName(int len) {
        var rng = ThreadLocalRandom.current();
        var sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int v = rng.nextInt(27);
            sb.append(v == 26 ? '_' : (char) ('A' + v));
        }
        return sb.toString();
    }
}
