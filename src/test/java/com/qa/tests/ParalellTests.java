package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Execution(ExecutionMode.CONCURRENT)
public class ParalellTests {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeEach
    void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--disable-dev-shm-usage")));
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    void testLoginPage() {
        page.navigate("https://the-internet.herokuapp.com/login");
        assertTrue(page.isVisible("h2:has-text('Login Page')"),
                "Заголовок 'Login Page' не найден");
    }

    @Test
    void testAddRemoveElements() {
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        page.navigate("https://the-internet.herokuapp.com/add_remove_elements/");
        page.click("button:text('Add Element')");
        assertTrue(page.isVisible("button.added-manually"));
    }

    @AfterEach
    void closeBrowser() {
        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}