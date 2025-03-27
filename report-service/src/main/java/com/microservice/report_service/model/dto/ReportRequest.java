package com.microservice.report_service.model.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ReportRequest {
    private Instant startDate;
    private Instant endDate;
    private String category;
    private String type;
}