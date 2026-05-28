package com.bharathisilks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoleAndCatalogTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JwtService jwt;

    private String owner;
    private String staff;

    @BeforeEach
    void tokens() {
        owner = tokenFor("OWNER");
        staff = tokenFor("STAFF");
    }

    private String tokenFor(String role) {
        AppUser u = new AppUser();
        u.setSubject("test-" + role);
        u.setName("Test " + role);
        u.setRole(role);
        u.setProvider("test");
        return jwt.generate(u);
    }

    @Test
    void staffCannotEditCatalogueButOwnerCan() throws Exception {
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Nope", "category", "Sarees", "price", 1200))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/products").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Owner Saree", "category", "Sarees", "price", 1200))))
                .andExpect(status().isCreated());
    }

    @Test
    void staffCanCompleteSales() throws Exception {
        String sku = create("Counter Kurti", "Kurtis", 500).path("sku").asText();
        mvc.perform(post("/api/sales").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "items", List.of(Map.of("sku", sku, "qty", 1)),
                                "pay", "Cash"))))
                .andExpect(status().isCreated());
    }

    @Test
    void addingCategoryDerivesPrefixForNewSkus() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Scarves"))))
                .andExpect(status().isCreated());

        JsonNode state = om.readTree(mvc.perform(get("/api/state").header("Authorization", "Bearer " + owner))
                .andReturn().getResponse().getContentAsString());
        boolean hasScarves = false;
        for (JsonNode c : state.path("categories")) {
            hasScarves |= "Scarves".equals(c.asText());
        }
        assertTrue(hasScarves, "new category should appear in state");

        String sku = create("Silk Scarf", "Scarves", 600).path("sku").asText();
        assertTrue(sku.matches("BS-SCA-\\d{4}"), "SKU prefix derived from category, was " + sku);

        // Staff may not add categories.
        mvc.perform(post("/api/categories").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", "Bags"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void importCreatesThenTopsUpStock() throws Exception {
        var rows = List.of(
                Map.of("name", "Imported Dupatta", "category", "Other", "cost", 200, "price", 450, "stock", 5),
                Map.of("name", "Imported Dupatta", "category", "Other", "cost", 200, "price", 450, "stock", 3));
        JsonNode result = om.readTree(mvc.perform(post("/api/products/import").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("rows", rows))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(1, result.path("created").asInt(), "first row creates");
        assertEquals(1, result.path("updated").asInt(), "duplicate row tops up");

        // Staff cannot import.
        mvc.perform(post("/api/products/import").header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("rows", rows))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditAndReportsAreOwnerOnly() throws Exception {
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + owner)).andExpect(status().isOk());
        mvc.perform(get("/api/reports").header("Authorization", "Bearer " + staff)).andExpect(status().isForbidden());
    }

    private JsonNode create(String name, String category, double price) throws Exception {
        String body = mvc.perform(post("/api/products").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", name, "category", category, "price", price, "cost", price / 2, "stock", 5))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body);
    }
}
