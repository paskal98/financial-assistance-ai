package com.microservice.report_service.repository;

import com.microservice.report_service.model.entity.ReportMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportMetadataRepository extends JpaRepository<ReportMetadata, UUID> {
    List<ReportMetadata> findByUserId(UUID userId);
}
