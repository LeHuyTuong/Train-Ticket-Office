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

    // ✅ Sinh mã giao dịch ngẫu nhiên
    public static String generateTxnRef() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // ✅ Mã hóa URL
    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ✅ Hàm băm SHA512
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

    // ✅ Tạo query string có chữ ký
    public static String buildSignedQuery(Map<String, String> fields, String secretKey) {
        SortedMap<String, String> sorted = new TreeMap<>(fields);
        StringBuilder query = new StringBuilder();
        StringBuilder signData = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;

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

        // 🪶 Debug: in ra để so sánh nếu bị lỗi chữ ký
        System.out.println("[VNPAY] signData=" + signData);
        System.out.println("[VNPAY] secureHash=" + secureHash);

        return query.toString();
    }

    // ✅ Xác thực chữ ký phản hồi
    public static boolean validateSignature(Map<String, String> fields, String secureHash, String secretKey) {
        SortedMap<String, String> sorted = new TreeMap<>(fields);
        StringBuilder signData = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if ("vnp_SecureHash".equals(entry.getKey()) || "vnp_SecureHashType".equals(entry.getKey()))
                continue;

            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;

            if (signData.length() > 0) {
                signData.append('&');
            }
            signData.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }

        String calculated = hmacSHA512(secretKey, signData.toString());
        System.out.println("[VNPAY] validate signData=" + signData);
        System.out.println("[VNPAY] provided=" + secureHash);
        System.out.println("[VNPAY] calculated=" + calculated);

        return calculated.equalsIgnoreCase(secureHash);
    }

    // ✅ Parse thời gian từ VNPAY
    public static LocalDateTime parsePayDate(String payDate) {
        if (payDate == null || payDate.isEmpty()) return null;
        return LocalDateTime.parse(payDate, VNPAY_DATE_FORMAT);
    }
}
