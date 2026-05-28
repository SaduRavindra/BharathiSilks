package com.bharathisilks.web.dto;

/** An uploaded invoice (base64) to be parsed into product rows by the AI service. */
public record AiImportRequest(String fileBase64, String mediaType) {
}
