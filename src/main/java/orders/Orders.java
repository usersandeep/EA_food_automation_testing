package orders;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class Orders {

    @BeforeClass
    public void beforeClass() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBaseUri("http://localhost:5000")
                .setContentType(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    private String createOrder() {
        String body = """
            {
              "productId": "p123",
              "quantity": 1,
              "slot": "Morning"
            }
            """;

        Response response = given()
                .body(body)
                .post("/orders");

        assertThat(response.statusCode(), equalTo(201));
        return response.jsonPath().getString("id");
    }

    @Test(groups = {"regression", "smoke"})
    public void cancelOrder_success() {
        // Step 1: Create Order
        String orderId = createOrder();

        // Step 2: Cancel Order
        Response response = given()
                .pathParam("id", orderId)
                .delete("/orders/{id}");

        System.out.println("==== Cancel Response ====");
        System.out.println(response.asPrettyString());

        // Step 3: Validate Response
        assertThat(response.statusCode(), equalTo(200));
        assertThat(response.jsonPath().getString("message"), equalTo("Order cancelled"));
    }

    @DataProvider(name = "invalidOrderIds")
    public Object[][] invalidOrderIds() {
        return new Object[][]{
                {"o0000000000000"},
                {"o9999999999999"},
                {"INVALID_ID"}
        };
    }

    @Test(dataProvider = "invalidOrderIds", groups = {"negative"})
    public void cancelOrder_invalidId(String invalidOrderId) {
        Response response = given()
                .pathParam("id", invalidOrderId)
                .delete("/orders/{id}");

        System.out.println("==== Invalid Cancel Response ====");
        System.out.println(response.asPrettyString());

        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("error"), containsString("Order not found"));
    }
}
