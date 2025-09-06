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

public class CancelRestoresStockTest {

    private RequestSpecification baseSpec;
    private final String baseUrl = System.getenv().getOrDefault("BASE_URL", "http://localhost:5000");
    private final String productId = System.getenv().getOrDefault("PRODUCT_ID", "p2");
    private final int qty = Integer.parseInt(System.getenv().getOrDefault("QTY", "2"));

    @BeforeClass
    public void beforeClass() {
        baseSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .build();
        System.out.println("Using BASE_URL = " + baseUrl);
    }

    @BeforeMethod
    public void beforeEachTest() {
        resetStockTo(10);
        clearAllOrders();
    }

    // --helpers 

    private RequestSpecification specWithSimHeader() {
        String sim = System.getProperty("SIMULATE_NOW", System.getenv().getOrDefault("SIMULATE_NOW", ""));
        if (sim == null || sim.trim().isEmpty()) return baseSpec;
        return baseSpec.header("X-Simulate-Now", sim.trim());
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

    // - test

    @Test
    public void cancelOrderRestoresStock() {
        System.out.println("\n=== Test: cancelOrderRestoresStock ===");

        Response pBefore = getProduct();
        Assert.assertEquals(pBefore.statusCode(), 200, "Should be able to fetch product before test");
        int beforeStock = pBefore.jsonPath().getInt("stock");
        System.out.println("Before stock: " + beforeStock);

        // Place an order for qty (from class field)
        String requested = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Response created = placeOrder("afternoon", requested);
        System.out.println("Create response status: " + created.statusCode());
        System.out.println(created.asPrettyString());
        Assert.assertEquals(created.statusCode(), 201, "Order create must return 201");

        String id = created.jsonPath().getString("id");

        Response pAfterPlace = getProduct();
        int afterPlaceStock = pAfterPlace.jsonPath().getInt("stock");
        System.out.println("After place stock: " + afterPlaceStock);

        // EXPECT that stock decreased by qty
        Assert.assertEquals(afterPlaceStock, beforeStock - qty, "Stock must decrease by ordered quantity after placing an order");

        // Cancel the order
        Response cancel = cancelOrder(id);
        System.out.println("Cancel response status: " + cancel.statusCode());
        System.out.println(cancel.asPrettyString());
        Assert.assertTrue(cancel.statusCode() == 200 || cancel.statusCode() == 204 || cancel.statusCode() == 202,
                "Cancel should return success status");

        Response pAfterCancel = getProduct();
        int afterCancelStock = pAfterCancel.jsonPath().getInt("stock");
        System.out.println("After cancel stock: " + afterCancelStock);

        // ASSERT stock restored to original
        Assert.assertEquals(afterCancelStock, beforeStock, "Stock should be restored to original value after cancel");

        System.out.println("=== End Test ===\n");
    }
}
