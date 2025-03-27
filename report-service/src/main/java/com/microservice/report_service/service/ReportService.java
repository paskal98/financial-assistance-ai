package com.microservice.report_service.service;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.ReportResponse;

import java.util.List;

public interface ReportService {

    ReportResponse generateReport(ReportRequest request, String token);

    List<ReportResponse> getReportHistory(String token);
}
