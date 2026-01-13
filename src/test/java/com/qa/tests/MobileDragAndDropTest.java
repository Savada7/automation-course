package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class MobileDragAndDropTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeEach
    void setup(){
        playwright = Playwright.create();

        Browser.NewContextOptions deviceOptions =  new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Linux; Android 12; SM-S908B) AppleWebKit/537.36" +
                        "(KHTML, like Gecko) Chrome/101.0.0.0 Mobile Safari/537.36")
                .setViewportSize(384, 873)
                .setDeviceScaleFactor(3.5)
                .setIsMobile(true)
                .setHasTouch(true);

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext(deviceOptions);
        page = context.newPage();
    }

    @Test
    void testDragAndDropMobile(){
        page.navigate("https://the-internet.herokuapp.com/drag_and_drop");

        Locator columnA = page.locator("#column-a");
        Locator columnB = page.locator("#column-b");

        assertEquals("A", columnA.locator("header").textContent());
        assertEquals("B", columnB.locator("header").textContent());

        try {
            columnA.dragTo(columnB);

            assertEquals("A", columnB.locator("header").textContent());
            assertEquals("B", columnA.locator("header").textContent());

        } catch (Exception e){
            System.out.println("Драг энд дроп не сработал");
        }
    }

    @AfterEach
    void tearDown(){
        if (page != null){
            page.close();
        }

        if (context != null){
            context.close();
        }

        if (browser != null){
            browser.close();
        }

        if (playwright != null){
            playwright.close();
        }
    }

}
