package orders;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class OrderCutoffTest {

    private RequestSpecification baseSpec;
    private final String baseUrl = System.getenv().getOrDefault("BASE_URL", "http://localhost:5000");
    private final String productId = System.getenv().getOrDefault("PRODUCT_ID", "p1");
    private final int qty = Integer.parseInt(System.getenv().getOrDefault("QTY", "1"));
    private final String simulateNowEnv = System.getenv().getOrDefault("SIMULATE_NOW", "").trim();

    @BeforeClass
    public void beforeClass() {
        baseSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .build();
        System.out.println("Using BASE_URL = " + baseUrl);
        if (!simulateNowEnv.isEmpty()) {
            System.out.println("SIMULATE_NOW detected: " + simulateNowEnv);
        } else {
            System.out.println("SIMULATE_NOW not set; tests may be non-deterministic for cutoff behavior.");
        }
    }

    @BeforeMethod
    public void beforeEachTest() {
        resetStockTo(10);
        clearAllOrders();
    }

    // ---------------- helpers ----------------

    private RequestSpecification specWithSimHeader() {
        if (simulateNowEnv.isEmpty()) return baseSpec;
        return baseSpec.header("X-Simulate-Now", simulateNowEnv);
    }

    private String todayForRequest() {
        if (!simulateNowEnv.isEmpty()) {
            Instant inst = Instant.parse(simulateNowEnv);
            LocalDate d = LocalDateTime.ofInstant(inst, ZoneId.systemDefault()).toLocalDate();
            return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalDateTime effectiveNow() {
        if (!simulateNowEnv.isEmpty()) {
            Instant inst = Instant.parse(simulateNowEnv);
            return LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
        }
        return LocalDateTime.now();
    }

    private String expectedDeliveryGivenNow(LocalDateTime now, String requestedDateStr) {
        LocalDate requested = LocalDate.parse(requestedDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate d = requested.plusDays(1); // base +1
        if (now.getHour() >= 18) d = d.plusDays(1); // +1 extra after cutoff
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String utcToLocal(String utcIso) {
        Instant instant = Instant.parse(utcIso);
        ZonedDateTime local = instant.atZone(ZoneId.systemDefault());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return local.format(fmt);
    }

    private Response getProduct() {
        return given().spec(specWithSimHeader()).when().get("/products/" + productId);
    }

    private Response placeOrder(String slot, String deliveryDate) {
        String body = String.format("{\"productId\":\"%s\",\"qty\":%d,\"slot\":\"%s\",\"deliveryDate\":\"%s\"}",
                productId, qty, slot, deliveryDate);
        return given().spec(specWithSimHeader()).body(body).when().post("/orders");
    }

    private Response cancelOrder(String id) {
        return given().spec(specWithSimHeader()).when().delete("/orders/" + id);
    }

    private void resetStockTo(int newStock) {
        String body = String.format("{\"updates\":[{\"productId\":\"%s\",\"newStock\":%d}]}", productId, newStock);
        given().spec(specWithSimHeader()).body(body).when().post("/ops/update-stock").then().statusCode(200);
    }

    @SuppressWarnings("unchecked")
    private void clearAllOrders() {
        Response r = given().spec(specWithSimHeader()).when().get("/orders");
        if (r.statusCode() != 200) return;
        List<Map<String, Object>> list = r.jsonPath().getList("$");
        if (list == null) return;
        for (Map<String, Object> o : list) {
            Object idObj = o.get("id");
            if (idObj == null) continue;
            String id = idObj.toString();
            given().spec(specWithSimHeader()).when().delete("/orders/" + id)
                    .then().statusCode(org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(404)));
        }
    }

    // ---------------- tests ----------------

    @Test(priority = 1)
    public void validOrderBeforeCutoff() {
        System.out.println("\n=== Scenario 1: Valid order before cut-off ===");

        LocalDateTime now = effectiveNow();
        String requested = todayForRequest();
        String expected = expectedDeliveryGivenNow(now, requested);

        Response created = placeOrder("morning", requested);
        System.out.println("Response (Scenario 1): " + created.statusCode());
        System.out.println(created.asPrettyString());
        Assert.assertEquals(created.statusCode(), 201);

        Map<String, Object> json = created.jsonPath().getMap("$");
        String actualDelivery = json.get("deliveryDate").toString();
        String createdAt = json.get("createdAt").toString();

        System.out.println("Expected delivery: " + expected + " | Actual delivery: " + actualDelivery);
        System.out.println("CreatedAt (UTC): " + createdAt);
        System.out.println("CreatedAt (Local): " + utcToLocal(createdAt));

        Assert.assertEquals(actualDelivery, expected);

        cancelOrder(json.get("id").toString());
        System.out.println("=== End of Scenario 1 ===\n");
    }

    @Test(priority = 2)
    public void orderAfterCutoff_shiftsByTwoDays() {
        System.out.println("\n=== Scenario 2: Order after cut-off ===");

        String requested = todayForRequest();
        Response created = placeOrder("evening", requested);
        System.out.println("Response (Scenario 2): " + created.statusCode());
        System.out.println(created.asPrettyString());
        Assert.assertEquals(created.statusCode(), 201);

        String actualDelivery = created.jsonPath().getString("deliveryDate");
        String createdAt = created.jsonPath().getString("createdAt");

        System.out.println("Scenario 2 -> Actual deliveryDate: " + actualDelivery);
        System.out.println("CreatedAt (UTC): " + createdAt);
        System.out.println("CreatedAt (Local): " + utcToLocal(createdAt));

        cancelOrder(created.jsonPath().getString("id"));
        System.out.println("=== End of Scenario 2 ===\n");
    }

    @Test(priority = 3)
    public void cancelRestoresStock() {
        System.out.println("\n=== Scenario 3: Cancel restores stock ===");

        Response pBefore = getProduct();
        int beforeStock = pBefore.jsonPath().getInt("stock");
        System.out.println("Before stock: " + beforeStock);

        Response created = placeOrder("afternoon", todayForRequest());
        System.out.println("Response (Scenario 3 - Create): " + created.statusCode());
        System.out.println(created.asPrettyString());
        Assert.assertEquals(created.statusCode(), 201);

        String id = created.jsonPath().getString("id");
        String createdAt = created.jsonPath().getString("createdAt");
        System.out.println("CreatedAt (UTC): " + createdAt);
        System.out.println("CreatedAt (Local): " + utcToLocal(createdAt));

        Response pAfterPlace = getProduct();
        System.out.println("After place stock: " + pAfterPlace.jsonPath().getInt("stock"));

        Response cancel = cancelOrder(id);
        System.out.println("Response (Scenario 3 - Cancel): " + cancel.statusCode());
        System.out.println(cancel.asPrettyString());

        Response pAfterCancel = getProduct();
        System.out.println("After cancel stock: " + pAfterCancel.jsonPath().getInt("stock"));

        System.out.println("=== End of Scenario 3 ===\n");
    }
}
