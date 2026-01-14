package com.ved.accessChecker;

import java.net.InetSocketAddress;
import java.net.Socket;

public class PortChecker {

    private static final int TIMEOUT_MS = 3000;

    public static String check(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            return "SUCCESS: " + host + ":" + port + " is reachable";
        } catch (Exception e) {
            System.out.checkError();
            System.out.println("Error while connecting : " + e.getMessage());
            return "FAILED : " + host + ":" + port + " ? " + e.getMessage();
        }
    }
}
