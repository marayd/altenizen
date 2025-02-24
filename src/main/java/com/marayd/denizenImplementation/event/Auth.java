package com.marayd.denizenImplementation.event;

import com.marayd.denizenImplementation.DenizenHook.StringProtector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Auth {
    private static final String AUTH_SERVER_URL = StringProtector.encrypt("https://api.kameoka.xyz/authorize");
    public static String AUTH_KEY;
    private static final String SERVER_ID = Auth.generateHardwareBasedID();

    protected static String generateHardwareBasedID() {
        try {
            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");
            String osVersion = System.getProperty("os.version");

            String cpuInfo = getCpuInfo();

            String serverName = InetAddress.getLocalHost().getHostName();

            String rawData = osName + osArch + osVersion + cpuInfo + serverName;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        }
        catch (UnknownHostException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static String getCpuInfo() {
        String cpuInfo = "";
        try {
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            cpuInfo = "CPU: " + availableProcessors + " cores";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cpuInfo;
    }
    public static boolean authorizeServer() {
        try {
            HttpURLConnection connection = getConnection();

            int statusCode = connection.getResponseCode();
            if (statusCode == 200) {
                return true;
            }

            try (InputStream is = connection.getErrorStream()) {
                byte[] errorBytes = is.readAllBytes();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static @NotNull HttpURLConnection getConnection() throws IOException {
        URL url = new URL(StringProtector.decrypt(AUTH_SERVER_URL));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonPayload = String.format(
                "{\"key\": \"%s\", \"server_id\": \"%s\"}",
                AUTH_KEY, SERVER_ID
        );

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

}
