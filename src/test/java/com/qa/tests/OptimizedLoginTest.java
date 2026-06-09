package com.qa.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptimizedLoginTest {
    static private Playwright playwright;
    static private Browser browser;
    private BrowserContext context;
    private Page page;
    static private List<Cookie> authCookies;

    @BeforeAll
    static void setUpClass(){
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

        try (BrowserContext tempContext = browser.newContext();
            Page tempPage = tempContext.newPage()){

            authCookies = performLogin(tempPage);
        }

    }

    @BeforeEach
    void setUp(){
        context = browser.newContext();
        context.addCookies(authCookies);
        page = context.newPage();
    }

    @Test
    void testSecureArea(){
        page.navigate("https://the-internet.herokuapp.com/secure");
        assertTrue(page.locator("h2").textContent().contains("Secure Area"));
    }

    @Test
    void testLogoutButtonExists() {
        page.navigate("https://the-internet.herokuapp.com/secure");
        // Проверяем наличие кнопки выхода
        assertTrue(page.locator("a.button:has-text('Logout')").isVisible());
    }

    private static List<Cookie> performLogin(Page page) {
        page.navigate("https://the-internet.herokuapp.com/login");

        // Заполняем форму входа
        page.locator("#username").fill("tomsmith");
        page.locator("#password").fill("SuperSecretPassword!");

        // Нажимаем кнопку входа
        page.locator("button[type='submit']").click();

        // Ждём перехода на защищённую страницу
        page.waitForURL("**/secure");

        // Возвращаем cookies после успешной аутентификации
        return page.context().cookies();
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
    }

    @AfterAll
    static void tearDownClass() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}

