package com.microservcie.ocr_service.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {
    private static final Logger logger = LoggerFactory.getLogger(CompressionUtils.class);
    private static final int COMPRESSION_THRESHOLD = 1024; // Сжимать текст больше 1 КБ

    public static String compress(String text) {
        if (text == null || text.length() < COMPRESSION_THRESHOLD) {
            return text; // Не сжимаем маленькие тексты
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            String compressed = Base64.getEncoder().encodeToString(baos.toByteArray());
            logger.debug("Text compressed: original size={}, compressed size={}", text.length(), compressed.length());
            return compressed;
        } catch (Exception e) {
            logger.error("Failed to compress text", e);
            throw new RuntimeException("Failed to compress OCR text", e);
        }
    }

    public static String decompress(String compressedText) {
        if (compressedText == null) {
            return null;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(compressedText));
             GZIPInputStream gzip = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            String decompressed = baos.toString(StandardCharsets.UTF_8);
            logger.debug("Text decompressed: compressed size={}, decompressed size={}", compressedText.length(), decompressed.length());
            return decompressed;
        } catch (Exception e) {
            logger.error("Failed to decompress text", e);
            throw new RuntimeException("Failed to decompress OCR text", e);
        }
    }
}