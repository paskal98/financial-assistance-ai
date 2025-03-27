package com.microservice.report_service.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedTransactionResponse {
    private List<TransactionResponse> content;
    private PageableInfo pageable;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private int size;
    private int number;

    @Data
    public static class PageableInfo {
        private int pageNumber;
        private int pageSize;
        private List<SortOrder> sort;
        private long offset;
        private boolean paged;
        private boolean unpaged;

        @Data
        public static class SortOrder {
            private String direction;
            private String property;
            private boolean ignoreCase;
            private String nullHandling;
            private boolean ascending;
        }
    }
}