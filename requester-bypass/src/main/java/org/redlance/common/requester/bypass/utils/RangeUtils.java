package org.redlance.common.requester.bypass.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RangeUtils {
    public static List<InetAddress> expandRanges(Iterable<String> ranges, int samplePrefix) {
        List<InetAddress> result = new ArrayList<>();
        for (String cidr : ranges) result.addAll(expandRange(cidr, samplePrefix));
        return result;
    }

    public static List<InetAddress> expandRange(String cidr, int samplePrefix) {
        String[] parts = cidr.split("/");
        int base = ipToInt(parts[0]);
        int prefix = Integer.parseInt(parts[1]);
        int bits = Math.max(samplePrefix, prefix);
        int step = 1 << (32 - bits);
        int count = 1 << (bits - prefix);

        List<InetAddress> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            try {
                result.add(InetAddress.getByAddress(intToBytes(base + i * step + 1)));
            } catch (UnknownHostException ignored) {}
        }
        return result;
    }

    public static int ipToInt(String ip) {
        String[] o = ip.split("\\.");
        return (Integer.parseInt(o[0]) << 24) | (Integer.parseInt(o[1]) << 16) | (Integer.parseInt(o[2]) << 8) | Integer.parseInt(o[3]);
    }

    public static byte[] intToBytes(int ip) {
        return new byte[]{(byte) (ip >> 24), (byte) (ip >> 16), (byte) (ip >> 8), (byte) ip};
    }
}
