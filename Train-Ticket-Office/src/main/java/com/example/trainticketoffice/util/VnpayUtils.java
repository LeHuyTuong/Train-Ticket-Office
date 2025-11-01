package com.example.trainticketoffice.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnpayUtils {

    public static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private VnpayUtils() {
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return hex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to sign data", e);
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String generateTxnRef() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public static String buildSignedQuery(Map<String, String> fields, String secretKey) {
        SortedMap<String, String> sorted = new TreeMap<>(fields);
        StringBuilder query = new StringBuilder();
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            String encodedKey = urlEncode(entry.getKey());
            String encodedValue = urlEncode(entry.getValue());
            if (query.length() > 0) {
                query.append('&');
                signData.append('&');
            }
            query.append(encodedKey).append('=').append(encodedValue);
            signData.append(encodedKey).append('=').append(encodedValue);
        }
        String secureHash = hmacSHA512(secretKey, signData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }

    public static boolean validateSignature(Map<String, String> fields, String secureHash, String secretKey) {
        SortedMap<String, String> sorted = new TreeMap<>(fields);
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("vnp_SecureHash".equals(entry.getKey()) || "vnp_SecureHashType".equals(entry.getKey())) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (signData.length() > 0) {
                signData.append('&');
            }
            signData.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        String calculated = hmacSHA512(secretKey, signData.toString());
        return calculated.equalsIgnoreCase(secureHash);
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static LocalDateTime parsePayDate(String payDate) {
        if (payDate == null || payDate.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(payDate, VNPAY_DATE_FORMAT);
    }
}
