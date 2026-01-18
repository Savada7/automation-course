package com.qa.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.*;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class HoverTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void setupClass(){
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
        );
    }

    @BeforeEach
    void setup(){
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    void testHoverProfiles(){
        page.navigate("https://the-internet.herokuapp.com/hovers",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

        Locator figures = page.locator(".figure");
        int count = figures.count();

        for (int i = 0; i < count; i++){
            System.out.println("Тестируем профиль " + (i + 1));

            Locator figure = figures.nth(i);

            figure.hover();

            Locator profileLink = figure.locator("text=View profile");
            assertTrue(profileLink.isVisible());

            page.waitForLoadState();

            Response response = page.waitForResponse(
                    Pattern.compile(".*/users/.*"),
                    ()-> profileLink.click()
            );

            String currentUrl = page.url();
            assertTrue(currentUrl.contains("/users/"));

            String[] urlParts = currentUrl.split("/users/");
            assertTrue(urlParts.length > 1);

            String userId = urlParts[1].replace("/", "").split("\\?")[0];
            assertTrue(userId.matches("\\d+"),
                    "получен ID " + userId);

            page.goBack();

            page.waitForSelector(".figure",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));
        }
    }
    @AfterEach
    void teardown(){
        if (context != null){
            context.close();
        }
    }
    @AfterAll
    static void teardownClass(){
        if (browser != null){
            browser.close();
        }
        if (playwright != null){
            playwright.close();
        }
    }
}
