package org.redlance.common.emotecraft.utils;

import java.util.UUID;

public class UuidCodec {
    private static final int BASE = 28;

    public static UUID encodeName(String name) {
        if (name.length() > 26) throw new IllegalArgumentException("Max 26 chars, got " + name.length());
        long msb = 0, lsb = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            int val = c == '_' ? 27 : c - 'A' + 1;
            // (msb, lsb) = (msb, lsb) * 28 + val
            long lsbLo = (lsb & 0xFFFFFFFFL) * BASE;
            long lsbHi = (lsb >>> 32) * BASE + (lsbLo >>> 32);
            lsb = (lsbLo & 0xFFFFFFFFL) | (lsbHi << 32);
            msb = msb * BASE + (lsbHi >>> 32);
            // add val
            long old = lsb;
            lsb += val;
            if (Long.compareUnsigned(lsb, old) < 0) msb++;
        }
        return new UUID(msb, lsb);
    }

    public static String decodeName(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        StringBuilder sb = new StringBuilder();
        while ((msb | lsb) != 0) {
            // (msb, lsb) / 28
            long rMsb = Long.remainderUnsigned(msb, BASE);
            msb = Long.divideUnsigned(msb, BASE);
            long mid = (rMsb << 32) | (lsb >>> 32);
            long rMid = Long.remainderUnsigned(mid, BASE);
            long qMid = Long.divideUnsigned(mid, BASE);
            long lo = (rMid << 32) | (lsb & 0xFFFFFFFFL);
            long qLo = Long.divideUnsigned(lo, BASE);
            int rem = (int) Long.remainderUnsigned(lo, BASE);
            lsb = (qMid << 32) | qLo;
            sb.append(rem == 27 ? '_' : (char) ('A' + rem - 1));
        }
        return sb.reverse().toString();
    }
}
