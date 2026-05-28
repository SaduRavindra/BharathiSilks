package com.bharathisilks.service;

import com.bharathisilks.web.dto.ProductRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Parses a supplier invoice (image or PDF) into product rows using the Anthropic
 * Messages API. Enabled only when {@code anthropic.api-key} is set; otherwise the
 * endpoint reports that AI import is unavailable (the static demo falls back to the
 * structured/paste flow). The parsed rows are returned for review, not auto-saved.
 */
@Service
public class AiImportService {

    private final String apiKey;
    private final String model;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public AiImportService(@Value("${anthropic.api-key:}") String apiKey,
                           @Value("${anthropic.model:claude-sonnet-4-6}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean enabled() {
        return !apiKey.isBlank();
    }

    public List<ProductRequest> parse(String fileBase64, String mediaType) {
        if (!enabled()) {
            throw new IllegalArgumentException("AI import isn't configured — set ANTHROPIC_API_KEY on the server");
        }
        if (fileBase64 == null || fileBase64.isBlank()) {
            throw new IllegalArgumentException("No file provided");
        }
        String mt = (mediaType == null || mediaType.isBlank()) ? "image/jpeg" : mediaType;
        String data = fileBase64.contains(",") ? fileBase64.substring(fileBase64.indexOf(',') + 1) : fileBase64;
        boolean pdf = mt.contains("pdf");

        String instructions = "Extract the purchasable line items from this supplier invoice. "
                + "Respond with ONLY a JSON array, no prose. Each element must be "
                + "{\"name\":string,\"category\":one of [Sarees,Lehengas,Dresses,Kurtis,Blouses,Other],"
                + "\"size\":string,\"color\":string,\"cost\":number,\"price\":number,\"stock\":integer}. "
                + "cost = unit purchase price from the invoice; if no selling price is shown set price to 0; "
                + "stock = quantity received; use \"\" or 0 when a field is unknown.";

        ObjectNode root = om.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 2000);
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        ObjectNode media = content.addObject();
        media.put("type", pdf ? "document" : "image");
        ObjectNode source = media.putObject("source");
        source.put("type", "base64");
        source.put("media_type", pdf ? "application/pdf" : mt);
        source.put("data", data);
        content.addObject().put("type", "text").put("text", instructions);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(Duration.ofSeconds(90))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(root)))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new IllegalArgumentException("AI service returned HTTP " + resp.statusCode());
            }
            String text = om.readTree(resp.body()).path("content").path(0).path("text").asText("");
            return toRows(text);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not reach the AI service: " + e.getMessage());
        }
    }

    private List<ProductRequest> toRows(String text) {
        String json = text.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        List<ProductRequest> rows = new ArrayList<>();
        try {
            JsonNode arr = om.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    rows.add(new ProductRequest(
                            n.path("name").asText(""),
                            n.path("category").asText("Other"),
                            n.path("size").asText(""),
                            n.path("color").asText(""),
                            n.path("cost").asDouble(0),
                            n.path("price").asDouble(0),
                            (int) n.path("stock").asDouble(0)));
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("AI returned an unreadable result");
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No line items found in the document");
        }
        return rows;
    }
}
