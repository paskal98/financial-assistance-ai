package com.microservice.report_service.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface ReportStorageService {
    String store(InputStream inputStream, String fileName, String contentType);
}
