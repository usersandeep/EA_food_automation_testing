package orders;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
public class AutomatePlaceOrderTest {
    @BeforeClass
    public void beforeClass() {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .setBaseUri("http://localhost:5000")
                .setContentType(ContentType.JSON)
                .log(LogDetail.ALL);

        RestAssured.requestSpecification = requestSpecBuilder.build();
    }
    @Test
    public void placeOrder_success() {
        String requestBody = """
                {
                  "productId": "p1",
                  "qty": 1,
                  "slot": "morning",
                  "deliveryDate": "2025-09-05"
                }""";
        Response response = with()
                .body(requestBody)
                .post("/orders");
        System.out.println("==== Full Response ====");
        System.out.println(response.asPrettyString());

        assertThat(response.statusCode(), equalTo(201));
        assertThat(response.jsonPath().getString("id"), notNullValue());
        assertThat(response.jsonPath().getString("productId"), equalTo("p1"));
        assertThat(response.jsonPath().getInt("qty"), equalTo(1));
        assertThat(response.jsonPath().getString("slot"), equalTo("morning"));
        assertThat(response.jsonPath().getString("status"), equalTo("delivered"));
        assertThat(response.jsonPath().getString("createdAt"), notNullValue());
    }
    @Test
    public void placeOrder_qtyZero() {
        String requestBody = """
                {
                  "productId": "p1",
                  "qty": 0,
                  "slot": "morning",
                  "deliveryDate": "2025-09-05"
                }""";
        Response response = with()
                .body(requestBody)
                .post("/orders");
        System.out.println("==== Full Response ====");
        System.out.println(response.asPrettyString());

        assertThat(response.statusCode(), equalTo(201));
        assertThat(response.jsonPath().getString("error"), containsString("Quantity must be greater than 0"));
    }
    @Test
    public void placeOrder_invalidSlot() {
        String requestBody = """
                {
                  "productId": "p1",
                  "qty": 1,
                  "slot": "midnight",
                  "deliveryDate": "2025-09-05"
                }""";
        Response response = with()
                .body(requestBody)
                .post("/orders");
        System.out.println("==== Full Response ====");
        System.out.println(response.asPrettyString());

        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("error"), containsString("Insufficient stock"));
    }
    @Test
    public void placeOrder_pastDate() {
        String requestBody = """
                {
                  "productId": "p1",
                  "qty": 1,
                  "slot": "morning",
                  "deliveryDate": "2023-01-01"
                }""";
        Response response = with()
                .body(requestBody)
                .post("/orders");
        System.out.println("==== Full Response ====");
        System.out.println(response.asPrettyString());

        assertThat(response.statusCode(), equalTo(400));
        assertThat(response.jsonPath().getString("error"), containsString("Insufficient stock"));
    }
}
