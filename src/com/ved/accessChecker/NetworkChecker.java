package com.ved.accessChecker;

import java.net.*;

public class NetworkChecker {

    public static Result hostReachable(String host, int timeout) throws MalformedURLException {

        Result https = tryHttp("https://" + host, timeout);
        if (https != null) return https;

        Result http = tryHttp("http://" + host, timeout);
        if (http != null) return http;

        int[] ports = {443, 80, 22, 8080};
        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeout);
                return new Result(
                        host,
                        true,
                        "HOST REACHABLE (TCP " + port + ")"
                );
            } catch (Exception ignored) {
            }
        }

        return new Result(
                host,
                false,
                "HOST UNREACHABLE (No route / timeout)"
        );
    }

    private static Result tryHttp(String urlStr, int timeout) throws MalformedURLException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.connect();

            return new Result(
                    url.getHost(),
                    true,
                    "HOST REACHABLE (" + url.getProtocol().toUpperCase() +
                            " " + conn.getResponseCode() + ")"
            );

        } catch (javax.net.ssl.SSLHandshakeException sslEx) {
            return new Result(
                    new URL(urlStr).getHost(),
                    true,
                    "HOST REACHABLE (SSL CERT ISSUE)"
            );
        } catch (Exception e) {
            return null;
        }
    }

    public static Result tcp(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return new Result(host + ":" + port, true, "PORT OPEN");
        } catch (Exception e) {
            return new Result(host + ":" + port, false, e.getMessage());
        }
    }
}
