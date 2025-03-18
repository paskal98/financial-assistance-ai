package com.microservice.auth_service.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
@Service
public class QRCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;

    public byte[] generateQRCode(String otpAuthUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUrl, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Ошибка генерации QR-кода", e);
            return null;
        }
    }

    public String generateQRCodeBase64(String otpAuthUrl) {
        byte[] qrCodeBytes = generateQRCode(otpAuthUrl);
        return qrCodeBytes != null ? Base64.getEncoder().encodeToString(qrCodeBytes) : null;
    }

    public void saveQRCodeToFile(String otpAuthUrl, String filePath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(otpAuthUrl, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        } catch (WriterException | IOException e) {
            log.error("Ошибка сохранения QR-кода в файл", e);
        }
    }
}
