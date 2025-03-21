package com.microservice.document_processing_service.service.ocr;

import com.microservice.document_processing_service.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.opencv.core.Core;
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

    private final FileDownloader fileDownloader;

    @Value("${tesseract.tessdata.path}")
    private String tessDataPath;

    @Value("${opencv.library.path:D:/opencv/build/java/x64}")
    private String opencvLibraryPath;

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
    public String extractText(InputStream inputStream, String objectName, UUID documentId, String contentType) {
        ITesseract tesseract = new Tesseract();

        try {
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
                logger.error("Tessdata directory not found at: {} for document: {}", tessDataPath, documentId);
                throw new DocumentProcessingException("Tessdata directory not found at: " + tessDataPath + " for document: " + documentId);
            }
            tesseract.setDatapath(tessDataPath);

            String result;
            if ("application/pdf".equals(contentType)) {
                result = extractTextFromPdf(inputStream, objectName, documentId);
            } else {
                logger.info("Extracting text from image: {} for document: {}", objectName, documentId);
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new DocumentProcessingException("Failed to read image from stream for document: " + documentId);
                }
                image = preprocessImage(image);
                result = tesseract.doOCR(image);
            }

            logger.info("Text extracted successfully for document {}: {}", documentId, result);
            return result;

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

    private String extractTextFromPdf(InputStream inputStream, String objectName, UUID documentId) throws Exception {
        if (inputStream.markSupported()) {
            inputStream.mark(Integer.MAX_VALUE);
        }

        try (PDDocument document = Loader.loadPDF(inputStreamToTempFile(inputStream, documentId))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text.trim().isEmpty()) {
                logger.warn("No text found in PDF, falling back to Tesseract OCR for document: {}", documentId);
                InputStream tesseractStream;
                if (inputStream.markSupported()) {
                    inputStream.reset();
                    tesseractStream = inputStream;
                } else {
                    logger.info("InputStream does not support mark/reset, re-downloading file: {} for document: {}", objectName, documentId);
                    tesseractStream = fileDownloader.downloadFile(objectName, documentId);
                }
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessDataPath);
                String result = tesseract.doOCR(inputStreamToTempFile(tesseractStream, documentId));
                if (!inputStream.markSupported()) {
                    tesseractStream.close();
                }
                return result;
            }
            return text;
        }
    }

    private BufferedImage preprocessImage(BufferedImage original) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(original, "png", baos);
        } catch (Exception e) {
            logger.error("Failed to convert BufferedImage to byte array", e);
            return original;
        }
        MatOfByte matOfByte = new MatOfByte(baos.toByteArray());
        Mat mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_GRAYSCALE);

        if (mat.width() < MIN_WIDTH || mat.height() < MIN_HEIGHT) {
            double scaleX = (double) MIN_WIDTH / mat.width();
            double scaleY = (double) MIN_HEIGHT / mat.height();
            double scale = Math.max(scaleX, scaleY);
            Imgproc.resize(mat, mat, new Size(mat.width() * scale, mat.height() * scale), 0, 0, Imgproc.INTER_LINEAR);
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

    private File inputStreamToTempFile(InputStream inputStream, UUID documentId) throws Exception {
        File tempFile = File.createTempFile("pdf-" + documentId, ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        tempFile.deleteOnExit();
        return tempFile;
    }
}