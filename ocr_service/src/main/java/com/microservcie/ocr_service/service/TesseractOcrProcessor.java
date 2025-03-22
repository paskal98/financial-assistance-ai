package com.microservcie.ocr_service.service;

import com.microservcie.ocr_service.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TesseractOcrProcessor implements OcrProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TesseractOcrProcessor.class);
    private static final int MIN_WIDTH = 300;
    private static final int MIN_HEIGHT = 300;

    @Value("${tesseract.tessdata.path}")
    private String tessDataPath;

    static {
        try {
            System.load("D:/opencv/build/java/x64/opencv_java470.dll"); // Укажи свой путь
            logger.info("OpenCV native library loaded successfully from: {}", "D:/opencv/build/java/x64/opencv_java470.dll");
        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load OpenCV native library from path. Ensure OpenCV is installed and the path is correct.", e);
            throw new RuntimeException("Failed to load OpenCV native library", e);
        }
    }

    @Override
    public String extractText(InputStream inputStream, String objectName, UUID documentId) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        String contentType = determineContentType(objectName);

        try {
            if ("application/pdf".equals(contentType)) {
                return extractTextFromPdf(inputStream, documentId, tesseract);
            } else {
                logger.info("Extracting text from image: {} for document: {}", objectName, documentId);
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new DocumentProcessingException("Failed to read image for document: " + documentId);
                }
                image = preprocessImage(image);
                return tesseract.doOCR(image);
            }
        } catch (Exception e) {
            logger.error("Failed to extract text from file: {} for document: {}", objectName, documentId, e);
            throw new DocumentProcessingException("Failed to process file with Tesseract for document: " + documentId, e);
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                logger.warn("Failed to close input stream for document: {}", documentId, e);
            }
        }
    }

    private String extractTextFromPdf(InputStream inputStream, UUID documentId, ITesseract tesseract) throws Exception {
        logger.info("Extracting text from PDF using Tesseract directly for document: {}", documentId);
        File tempFile = createTempPdfFile(inputStream, documentId);
        try {
            String result = tesseract.doOCR(tempFile);
            String trimmedResult = result.trim();
            if (trimmedResult.isEmpty()) {
                throw new DocumentProcessingException("No text extracted from PDF for document: " + documentId);
            }
            logger.info("Extracted text length: {} for document: {}", trimmedResult.length(), documentId);
            return trimmedResult;
        } finally {
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    logger.warn("Failed to delete temporary file: {} for document: {}", tempFile.getAbsolutePath(), documentId);
                }
            }
        }
    }

    private File createTempPdfFile(InputStream inputStream, UUID documentId) throws Exception {
        File tempFile = File.createTempFile("pdf-" + documentId, ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            logger.error("Failed to create temporary PDF file for document: {}", documentId, e);
            throw new DocumentProcessingException("Failed to create temporary PDF file for document: " + documentId, e);
        }
        tempFile.deleteOnExit(); // Удаление при завершении JVM, если не удалится вручную
        return tempFile;
    }

    private BufferedImage preprocessImage(BufferedImage original) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(original, "png", baos);
        } catch (Exception e) {
            logger.error("Failed to convert BufferedImage to byte array", e);
            return original;
        }
        Mat mat = Imgcodecs.imdecode(new MatOfByte(baos.toByteArray()), Imgcodecs.IMREAD_GRAYSCALE);

        if (mat.width() < MIN_WIDTH || mat.height() < MIN_HEIGHT) {
            double scale = Math.max((double) MIN_WIDTH / mat.width(), (double) MIN_HEIGHT / mat.height());
            Imgproc.resize(mat, mat, new Size(mat.width() * scale, mat.height() * scale));
            logger.info("Image scaled to {}x{}", mat.width(), mat.height());
        }

        Imgproc.equalizeHist(mat, mat);
        Imgproc.medianBlur(mat, mat, 3);

        byte[] bytes = new byte[(int) (mat.total() * mat.channels())];
        mat.get(0, 0, bytes);
        BufferedImage processedImage = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_BYTE_GRAY);
        processedImage.getRaster().setDataElements(0, 0, mat.width(), mat.height(), bytes);
        return processedImage;
    }

    private String determineContentType(String filePath) {
        if (filePath.endsWith(".pdf")) return "application/pdf";
        else if (filePath.endsWith(".png")) return "image/png";
        else if (filePath.endsWith(".jpeg") || filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}