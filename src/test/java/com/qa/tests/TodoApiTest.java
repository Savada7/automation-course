package com.qa.tests;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TodoApiTest {
    Playwright playwright;
    APIRequestContext requestContext;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(){
        playwright = Playwright.create();
        requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("https://jsonplaceholder.typicode.com")
        );
    }

    @Test
    void testTodoBaseEndPoint() throws Exception{
        APIResponse response = requestContext.get("/");

        assertEquals(200, response.status());
    }
    // Так как базовый эндпоинт возвращает ответ только HTML проверяем только статус, без парсинга

    @Test
    void testTodoApi() throws Exception{
        APIResponse response = requestContext.get("/todos/1");

        assertEquals(200, response.status());

        String responseBody = response.text();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        assertTrue(jsonNode.has("userId"));
        assertTrue(jsonNode.has("id"));
        assertTrue(jsonNode.has("title"));
        assertTrue(jsonNode.has("completed"));

        assertTrue(jsonNode.get("userId").isInt());
        assertTrue(jsonNode.get("id").isInt());
        assertTrue(jsonNode.get("title").isTextual());
        assertTrue(jsonNode.get("completed").isBoolean());
    }

    @AfterEach
    void tearDown(){
        try {
            if (requestContext != null){
                requestContext.dispose();
            }
        } catch (Exception e){
            // Игнор искоючения
        }

        try {
            if (playwright !=null){
                playwright.close();
            }
        }catch (Exception e){
            //Игнор исключения
        }
    }
}
