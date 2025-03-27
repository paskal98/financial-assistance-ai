package com.microservice.report_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "report_metadata")
@Data
public class ReportMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(nullable = false)
    private String format;

    @Column(nullable = false)
    private Instant generatedAt;

    private Instant startDate;
    private Instant endDate;

    @Column(columnDefinition = "TEXT")
    private String categories;

    private String type;
}
