package de.berlin.htw;


import org.junit.jupiter.api.Test;


import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndIntegrationTest {

    private static final int TX_PORT = 8081;
    private static final int TRANSFER_PORT = 8084;


    @Test
    void transactionFlowsThroughAllServices() throws Exception {

        String transactionId = UUID.randomUUID().toString();

        String txJson = String.format("""
{
    "transactionId": "%s",
    "fromAccount": "IT-AA",
    "toAccount": "IT-BB",
    "amount": 2,
    "currency": "EUR"
}
""", transactionId);


        given()
                .baseUri("http://localhost")
                .port(TX_PORT)
                .accept("*/*")
                .contentType("application/json")
                .body(txJson)
                .when()
                .post("/api/transactions")
                .then()
                .statusCode(202);

        boolean arrived = waitForTransaction("IT-BB", transactionId, Duration.ofSeconds(10));

        assertTrue(arrived, "Transaction should be visible in transfer-service history");
    }

    private boolean waitForTransaction(String accountId, String txId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            var resp = given()
                    .baseUri("http://localhost")
                    .port(TRANSFER_PORT)
                    .when()
                    .get("/api/accounts/" + accountId + "/transactions")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath();

            List<String> ids = resp.getList("transactions.transactionId");
            return ids != null && ids.size() == 1 && ids.contains(txId);

        }
        return false;
    }
}

