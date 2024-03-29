package org.aldettinger;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class QuarkusConfigureBeforeObjectInitializationTest {

    @Test
    void quarkusConfigAreSetAfterObjectInitialization() {
        RestAssured.get("/hello").then().statusCode(200).body(is("BasicMessage :: MyBean"));
    }

}
