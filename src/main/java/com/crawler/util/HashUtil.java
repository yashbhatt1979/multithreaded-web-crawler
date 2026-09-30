package com.crawler.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

    private HashUtil() {
        // Utility class
    }

    public static String sha256(String value) {

        if (value == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes =
                    digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}