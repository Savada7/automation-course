package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusCodeApiUiTest {
    private Playwright playwright;
    private APIRequestContext apiRequest;
    private Browser browser;
    private Page page;

    @BeforeEach
    void setUp(){
        playwright = Playwright.create();

        apiRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("https://the-internet.herokuapp.com")
        );

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );

        page = browser.newPage();

        page.navigate("https://the-internet.herokuapp.com/status_codes");
        page.waitForSelector("div.example");
    }

    @Test
    void testStatusCodesCombined(){

        int apiStatusCode200 = getApiStatusCode(200);
        int uiStatusCode200 = getUiStatusCode(200);

        assertEquals( apiStatusCode200, uiStatusCode200,
                "Status codes for 200 should match");

        int apiStatusCode404 = getApiStatusCode(404);
        int uiStatusCode404 = getUiStatusCode(404);

        assertEquals(apiStatusCode404, uiStatusCode404,
                "Status codes for 404 should match");

    }

    private int getApiStatusCode(int code){
        APIResponse response = apiRequest.get("/status_codes/" + code);
        return response.status();
    }

    private int getUiStatusCode(int code){
        try {
            Locator link = page.locator("text=" + code).first();

            Response response = page.waitForResponse(
                    res -> res.url().endsWith("/status_codes/" + code),
                    () -> link.click(new Locator.ClickOptions().setTimeout(15000))
            );

            page.goBack();
            return response.status();

        } catch (Exception e) {
            System.err.println("Error getting UI status code for " + code + ": " + e.getMessage());
            return -1;
        }
    }

    @AfterEach
    void teardown(){
        if (apiRequest != null){
            apiRequest.dispose();
        }

        if (page != null){
            page.close();
        }

        if (browser != null){
            browser.close();
        }

        if (playwright != null){
            playwright.close();
        }
    }
}
