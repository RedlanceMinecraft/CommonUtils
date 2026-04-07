package org.redlance.common.requester.bypass.bgp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"SameParameterValue", "unused"}) // API
public class BgpRangeFetcher {
    private static final Pattern CIDR_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+)");
    private static final InetSocketAddress WHOIS_ADDRESS = new InetSocketAddress("whois.radb.net", 43);

    public static void fetchBgpRanges(String origin, Predicate<String> onRange) throws IOException {
        BgpRangeFetcher.fetchBgpRanges(WHOIS_ADDRESS, origin, onRange);
    }

    public static void fetchBgpRanges(InetSocketAddress address, String origin, Predicate<String> onRange) throws IOException {
        try (Socket sock = new Socket()) {
            sock.connect(address, 10000);
            sock.setSoTimeout(15000);

            sock.getOutputStream().write(("-T route -K -i origin " + origin + "\r\n").getBytes(StandardCharsets.US_ASCII));
            sock.getOutputStream().flush();

            Set<String> seen = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8), 65536)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.interrupted()) return;

                    if (line.startsWith("route:")) {
                        Matcher m = CIDR_PATTERN.matcher(line);

                        if (m.find() && seen.add(m.group(1))) {
                            if (!onRange.test(m.group(1))) return;
                        }
                    }
                }
            }
        }
    }
}
