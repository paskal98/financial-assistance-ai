package com.microservice.report_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ReportResponse {
    private UUID reportId;
    private String fileUrl;
    private String status;
}
