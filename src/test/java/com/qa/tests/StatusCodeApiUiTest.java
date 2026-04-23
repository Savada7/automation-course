package com.qa.tests;

import com.microsoft.playwright.*;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.qa.config.EnvConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusCodeApiUiTest {
    private Playwright playwright;
    private APIRequestContext apiRequest;
    private Browser browser;
    private Page page;
    private static EnvConfig config;

    @BeforeAll
    static void loadConfig() {
        config = ConfigFactory.create(EnvConfig.class, System.getProperties());
    }

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();

        apiRequest = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(config.baseUrl().replaceAll("/$", ""))
        );

        BrowserType browserType = getBrowserType(playwright, config.browser());
        browser = browserType.launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(config.headless())
        );

        page = browser.newPage();
        page.navigate(config.baseUrl() + "/status_codes");
        page.waitForSelector("div.example");
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 404})
    void testStatusCodeCombined(int statusCode) {
        int apiStatusCode = getApiStatusCode(statusCode);
        int uiStatusCode = getUiStatusCode(statusCode);

        assertEquals(apiStatusCode, uiStatusCode,
                String.format("Status code %d mismatch - API: %d, UI: %d",
                        statusCode, apiStatusCode, uiStatusCode));
    }

    private BrowserType getBrowserType(Playwright playwright, String browserName) {
        switch (browserName.toLowerCase()) {
            case "firefox": return playwright.firefox();
            case "webkit": return playwright.webkit();
            default: return playwright.chromium();
        }
    }

    private int getApiStatusCode(int code) {
        APIResponse response = apiRequest.get("/status_codes/" + code);
        return response.status();
    }

    private int getUiStatusCode(int code) {
        Locator link = page.locator("a[href*='status_codes/" + code + "']").first();

        Response response = page.waitForResponse(
                res -> res.url().endsWith("/status_codes/" + code),
                () -> link.click(new Locator.ClickOptions().setTimeout(15000))
        );

        page.goBack();
        page.waitForSelector("div.example");
        return response.status();
    }

    @AfterEach
    void teardown() {
        if (apiRequest != null) apiRequest.dispose();
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}