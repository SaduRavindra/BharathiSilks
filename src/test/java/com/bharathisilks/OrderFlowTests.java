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
class OrderFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JwtService jwt;

    private String staff;

    @BeforeEach
    void signIn() {
        AppUser u = new AppUser();
        u.setSubject("test-staff");
        u.setName("Counter Staff");
        u.setRole("STAFF");
        u.setProvider("test");
        staff = jwt.generate(u);
    }

    private String firstSku() throws Exception {
        String body = mvc.perform(get("/api/public/products"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(body).get(0).path("sku").asText();
    }

    private JsonNode place(String sku, int qty) throws Exception {
        String body = mvc.perform(post("/api/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Test Buyer", "phone", "9991110000",
                                "fulfilment", "Delivery", "address", "1 Test Street",
                                "items", List.of(Map.of("sku", sku, "qty", qty))))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return om.readTree(body);
    }

    @Test
    void publicCanPlaceAndTrackOrder() throws Exception {
        JsonNode order = place(firstSku(), 2);
        String ref = order.path("ref").asText();
        assertTrue(ref.startsWith("ORD-"), "a tracking ref is issued");
        assertEquals("PLACED", order.path("status").asText());
        assertEquals(2, order.path("items").get(0).path("qty").asInt());
        assertTrue(order.path("total").asDouble() > 0, "total priced server-side");

        JsonNode tracked = om.readTree(mvc.perform(get("/api/public/orders/" + ref))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(ref, tracked.path("ref").asText());
        assertEquals(1, tracked.path("timeline").size(), "timeline starts at PLACED");
    }

    @Test
    void orderManagementRequiresAuthAndStaffCanAdvanceStatus() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());

        String ref = place(firstSku(), 1).path("ref").asText();

        JsonNode list = om.readTree(mvc.perform(get("/api/orders").header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(list.size() >= 1, "staff can list orders");

        JsonNode updated = om.readTree(mvc.perform(post("/api/orders/" + ref + "/status")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "confirmed"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("CONFIRMED", updated.path("status").asText());
        assertEquals(2, updated.path("timeline").size(), "status change appends to timeline");

        mvc.perform(post("/api/orders/" + ref + "/status")
                        .header("Authorization", "Bearer " + staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "NOPE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fulfilBillsTheOrderDecrementsStockAndIsIdempotent() throws Exception {
        String sku = firstSku();
        int before = stockOf(sku);
        String ref = place(sku, 1).path("ref").asText();

        JsonNode order = om.readTree(mvc.perform(post("/api/orders/" + ref + "/fulfil")
                        .header("Authorization", "Bearer " + staff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("DELIVERED", order.path("status").asText());
        assertTrue(order.path("saleInv").asText().matches("INV-\\d+"), "an invoice is recorded");
        assertEquals(before - 1, stockOf(sku), "stock decremented on fulfilment");

        mvc.perform(post("/api/orders/" + ref + "/fulfil").header("Authorization", "Bearer " + staff))
                .andExpect(status().isBadRequest());
    }

    private int stockOf(String sku) throws Exception {
        String body = mvc.perform(get("/api/products").header("Authorization", "Bearer " + staff))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode p : om.readTree(body)) {
            if (sku.equals(p.path("sku").asText())) {
                return p.path("stock").asInt();
            }
        }
        return -1;
    }
}
