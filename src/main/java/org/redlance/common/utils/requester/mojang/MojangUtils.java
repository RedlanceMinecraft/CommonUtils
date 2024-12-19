package org.redlance.common.utils.requester.mojang;

public class MojangUtils {
    public static String parseUuid(String id) {
        if (id.length() != 32) {
            return id;
        }

        String first = id.substring(0, 8);
        String second = id.substring(8, 12);
        String third = id.substring(12, 16);
        String fourth = id.substring(16, 20);
        String fifth = id.substring(20);

        return first + "-" + second + "-" + third + "-" + fourth + "-" + fifth;
    }
}
