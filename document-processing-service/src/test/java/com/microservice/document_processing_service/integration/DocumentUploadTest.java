package com.microservice.document_processing_service.integration;

import com.microservice.document_processing_service.model.entity.Document;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class DocumentUploadTest extends BaseIntegrationTest {

    @Test
    void uploadDocument_Success() {
        File testFile = new File("src/test/resources/test.pdf");

        given()
                .header("Authorization", "Bearer mock-token")
                .multiPart("files", testFile, "application/pdf")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/documents/upload")
                .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .body(containsString("Document queued for processing"));
    }



    @Test
    void uploadDocument_UnsupportedFileType() {
        File testFile = new File("src/test/resources/test.txt");

        given()
                .header("Authorization", "Bearer mock-token")
                .multiPart("files", testFile, "text/plain")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/documents/upload")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("Validation error"))
                .body("details", containsString("Unsupported file type: text/plain"))
                .body("status", equalTo(400));
    }

    @Test
    void uploadDocument_NoFiles() {
        given()
                .header("Authorization", "Bearer mock-token")
                .multiPart("files", "", "")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/documents/upload")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(containsString("Missing required part: files"));
    }



    @Test
    void getDocumentStatus_Success() {
        UUID documentId = UUID.randomUUID();
        UUID userId = this.userId;

        Document document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setFilePath("somefile.pdf");
        document.setStatus("PROCESSING");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        documentRepository.save(document);

        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/documents/" + documentId + "/status")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("PROCESSING"));
    }

    @Test
    void uploadDocument_FileTooLarge() {
        File largeFile = new File("src/test/resources/large.pdf");

        given()
                .header("Authorization", "Bearer mock-token")
                .multiPart("files", largeFile, "application/pdf")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/documents/upload")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(containsString("File exceeds size limit of 5MB"));
    }

    @Test
    void uploadMultipleDocuments_Success() {
        File testFile1 = new File("src/test/resources/test.pdf");
        File testFile2 = new File("src/test/resources/test.pdf");

        given()
                .header("Authorization", "Bearer mock-token")
                .multiPart("files", testFile1, "application/pdf")
                .multiPart("files", testFile2, "application/pdf")
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/documents/upload")
                .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .body("[0]", containsString("Document queued for processing"))
                .body("[1]", containsString("Document queued for processing"))
                .body("size()", equalTo(2));
    }

    @Test
    void getDocumentStatus_WrongUser_Forbidden() {
        UUID documentId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID(); // Другой пользователь

        Document document = new Document();
        document.setId(documentId);
        document.setUserId(otherUserId); // Документ принадлежит другому пользователю
        document.setFilePath("somefile.pdf");
        document.setStatus("PROCESSING");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        documentRepository.save(document);

        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/documents/" + documentId + "/status")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("message", equalTo("Access denied"))
                .body("details", equalTo("You do not own this document"))
                .body("status", equalTo(403));
    }

    @Test
    void getDocumentStatus_DocumentNotFound() {
        UUID nonExistentDocumentId = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer mock-token")
                .when()
                .get("/documents/" + nonExistentDocumentId + "/status")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body(containsString("Document not found: " + nonExistentDocumentId));
    }


}