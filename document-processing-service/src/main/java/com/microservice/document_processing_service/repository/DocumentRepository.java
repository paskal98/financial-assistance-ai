package com.microservice.document_processing_service.repository;

import com.microservice.document_processing_service.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}