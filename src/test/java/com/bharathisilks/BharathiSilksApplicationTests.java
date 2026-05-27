package com.bharathisilks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BharathiSilksApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    void contextLoadsAndSeedsCatalogue() throws Exception {
        String body = mvc.perform(get("/api/state"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode products = om.readTree(body).path("products");
        assertTrue(products.size() >= 6, "demo catalogue should be seeded");
        assertNotNull(findBySku(products, "BS-SAR-0001"), "first saree SKU should exist");
    }

    @Test
    void createAssignsSkuAndGstByPrice() throws Exception {
        JsonNode premium = create("Premium Silk", "Sarees", 1200, 800, 5);
        assertTrue(premium.path("sku").asText().matches("BS-SAR-\\d{4}"));
        assertEquals(12, premium.path("gst").asInt(), "above Rs.1000 is 12% GST");

        JsonNode budget = create("Budget Kurti", "Kurtis", 900, 500, 5);
        assertEquals(5, budget.path("gst").asInt(), "Rs.1000 or below is 5% GST");
    }

    @Test
    void completeSaleComputesTotalsAndDecrementsStock() throws Exception {
        String sku = create("POS Test Saree", "Kurtis", 500, 300, 10).path("sku").asText();

        JsonNode sale = postSale(Map.of(
                "items", List.of(Map.of("sku", sku, "qty", 2)),
                "pay", "Cash"));

        assertEquals(1000.0, sale.path("sub").asDouble());
        assertEquals(50.0, sale.path("tax").asDouble(), "5% of 1000");
        assertEquals(1050.0, sale.path("total").asDouble());
        assertEquals(400.0, sale.path("profit").asDouble(), "(500-300) x 2");
        assertTrue(sale.path("inv").asText().matches("INV-\\d{5}"));

        assertEquals(8, stockOf(sku), "stock should drop by 2");
    }

    @Test
    void percentDiscountAndLoyaltyArePricedServerSide() throws Exception {
        String phone = "9990000001";
        String sku = create("Loyalty Saree", "Kurtis", 1000, 600, 5).path("sku").asText();

        JsonNode sale = postSale(Map.of(
                "items", List.of(Map.of("sku", sku, "qty", 1)),
                "phone", phone,
                "name", "Asha",
                "disc", 10,
                "discType", "%",
                "pay", "UPI"));

        assertEquals(1000.0, sale.path("sub").asDouble());
        assertEquals(100.0, sale.path("disc").asDouble(), "10% of 1000");
        assertEquals(50.0, sale.path("tax").asDouble());
        assertEquals(950.0, sale.path("total").asDouble(), "1000 - 100 + 50");

        JsonNode customer = om.readTree(mvc.perform(get("/api/customers/" + phone))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(4, customer.path("points").asInt(), "floor(950 / 200)");
        assertEquals(950.0, customer.path("spend").asDouble());
        assertEquals(1, customer.path("visits").asInt());
    }

    @Test
    void returnRestocksAndReversesLoyalty() throws Exception {
        String phone = "9990000002";
        String sku = create("Return Saree", "Kurtis", 400, 200, 4).path("sku").asText();

        JsonNode sale = postSale(Map.of(
                "items", List.of(Map.of("sku", sku, "qty", 3)),
                "phone", phone,
                "pay", "Card"));
        String inv = sale.path("inv").asText();
        assertEquals(1, stockOf(sku));

        JsonNode returned = om.readTree(mvc.perform(post("/api/sales/" + inv + "/return"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertTrue(returned.path("returned").asBoolean());
        assertEquals(4, stockOf(sku), "items go back to stock");

        JsonNode customer = om.readTree(mvc.perform(get("/api/customers/" + phone))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertEquals(0.0, customer.path("spend").asDouble(), "spend reversed");
        assertEquals(0, customer.path("points").asInt(), "points reversed");
    }

    @Test
    void sellingMoreThanStockIsRejected() throws Exception {
        String sku = create("Scarce Saree", "Kurtis", 500, 300, 1).path("sku").asText();
        mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "items", List.of(Map.of("sku", sku, "qty", 5)),
                                "pay", "Cash"))))
                .andExpect(status().isBadRequest());
    }

    private JsonNode create(String name, String category, double price, double cost, int stock)
            throws Exception {
        String body = mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", name, "category", category,
                                "price", price, "cost", cost, "stock", stock))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body);
    }

    private JsonNode postSale(Map<String, Object> payload) throws Exception {
        String body = mvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body);
    }

    private int stockOf(String sku) throws Exception {
        String body = mvc.perform(get("/api/products"))
                .andReturn().getResponse().getContentAsString();
        JsonNode product = findBySku(om.readTree(body), sku);
        assertNotNull(product, "product " + sku + " should exist");
        return product.path("stock").asInt();
    }

    private JsonNode findBySku(JsonNode products, String sku) {
        for (JsonNode node : products) {
            if (sku.equals(node.path("sku").asText())) {
                return node;
            }
        }
        return null;
    }
}
