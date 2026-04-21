package com.qa.example;

import com.qa.example.config.EnvironmentConfig;
import com.microsoft.playwright.*;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class StatusCodeTest {

    private static Playwright playwright;
    private static Browser browser;
    private Page page;
    private BrowserContext context;
    private EnvironmentConfig config;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @BeforeEach
    void setUp() {
        // Получаем env из переменной окружения или system property
        String env = System.getenv("env");
        if (env == null || env.isEmpty()) {
            env = System.getProperty("env", "dev");
        }

        // Загружаем конфиг
        config = ConfigFactory.create(EnvironmentConfig.class,
                System.getProperties(),
                System.getenv());

        context = browser.newContext();
        page = context.newPage();

    }

    @Test
    void testStatusCode200() {
        String url = config.baseUrl() + "/status_codes/200";
        Response response = page.navigate(url);
        assertEquals(200, response.status());
    }

    @Test
    void testStatusCode404() {
        String url = config.baseUrl() + "/status_codes/404";
        Response response = page.navigate(url);
        assertEquals(404, response.status());
    }

    @Test
    void testStatusCode500() {
        String url = config.baseUrl() + "/status_codes/500";
        Response response = page.navigate(url);
        assertEquals(500, response.status());
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}