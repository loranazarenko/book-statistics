package com.profitsoft.application.dto;

import lombok.Data;

@Data
public class BookImportResultDto {
    private int successCount;
    private int failedCount;
}
