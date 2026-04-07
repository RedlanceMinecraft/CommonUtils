package org.redlance.common.requester.bypass.utils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;

@SuppressWarnings("unused") // API
public class DirectTlsProbe {
    private static final SSLContext TRUST_ALL = createTrustAllContext();

    public static int probe(InetAddress ip, String hostname, int port, String path) throws IOException {
        return DirectTlsProbe.probe(ip, hostname, port, path, 35000, 3000);
    }

    public static int probe(InetAddress ip, String hostname, int port, String path, int minBytes, int timeout) throws IOException {
        try (Socket raw = new Socket()) {
            raw.connect(new InetSocketAddress(ip, port), timeout);

            if (Thread.interrupted()) throw new IOException("Interrupted");

            raw.setSoTimeout(timeout);

            SSLSocket ssl = (SSLSocket) TRUST_ALL.getSocketFactory().createSocket(raw, hostname, port, true);
            SSLParameters params = ssl.getSSLParameters();
            params.setServerNames(List.of(new SNIHostName(hostname)));
            ssl.setSSLParameters(params);
            ssl.startHandshake();
            ssl.setSoTimeout(timeout);

            ssl.getOutputStream().write((
                    "GET " + path + " HTTP/1.1\r\n" +
                            "Host: " + hostname + "\r\n" +
                            "Connection: close\r\n\r\n"
            ).getBytes(StandardCharsets.US_ASCII));

            int total = 0;
            byte[] buf = new byte[8192];
            int n;
            while ((n = ssl.getInputStream().read(buf)) != -1) {
                total += n;
                if (total >= minBytes || Thread.interrupted()) break;
            }

            // ssl.close();
            return total;
        }
    }

    private static SSLContext createTrustAllContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] c, String t) {}

                @Override
                public void checkServerTrusted(X509Certificate[] c, String t) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
