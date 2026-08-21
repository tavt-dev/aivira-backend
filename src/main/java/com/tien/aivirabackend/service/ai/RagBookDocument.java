package com.tien.aivirabackend.service.ai;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import org.jsoup.Jsoup;

import com.tien.aivirabackend.domain.entity.catalog.Product;

public record RagBookDocument(long productId, String title, String content, String contentHash,
        Map<String, Object> payload) {
    public static RagBookDocument from(Product product, String provider, String model) {
        String category = product.getCategory() == null ? "" : safe(product.getCategory().getCategoryName());
        String content = String.join("\n", "Title: " + safe(product.getProductName()),
                "Author: " + safe(product.getBookAuthor()), "Category: " + category,
                "Publisher: " + safe(product.getPublisher()), "ISBN: " + safe(product.getIsbn()),
                "Language: " + safe(product.getBookLanguage()), "Format: " + String.valueOf(product.getBookFormat()),
                "Publication year: " + String.valueOf(product.getPublicationYear()),
                "Description: " + Jsoup.parse(safe(product.getDescription())).text());
        String hash = sha256(content);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getId());
        payload.put("active", Boolean.TRUE.equals(product.getActive()));
        payload.put("status", String.valueOf(product.getStatus()));
        payload.put("categoryId", product.getCategory() == null ? 0 : product.getCategory().getId());
        payload.put("language", safe(product.getBookLanguage()));
        payload.put("price", decimal(product.getPrice()));
        payload.put("contentHash", hash);
        payload.put("provider", provider);
        payload.put("model", model);
        payload.put("indexedAt", Instant.now().toString());
        return new RagBookDocument(product.getId(), product.getProductName(), content, hash, payload);
    }

    private static double decimal(BigDecimal value) { return value == null ? 0 : value.doubleValue(); }
    private static String safe(Object value) { return value == null ? "" : value.toString(); }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}

