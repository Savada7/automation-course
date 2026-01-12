package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MobileDynamicControlsTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeEach
    void SetUp(){
        playwright = Playwright.create();

        Browser.NewContextOptions deviceOptions = new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.14" +
                        "(KHTML, like Gecko)")
                .setViewportSize(834, 1194)
                .setDeviceScaleFactor(2)
                .setIsMobile(true)
                .setHasTouch(true);

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext(deviceOptions);
        page = context.newPage();
    }

    @Test
    void testInputEnabling(){
        page.navigate("https://the-internet.herokuapp.com/dynamic_controls");

//Проверяем что поле изначально неактивно
        Locator inputField = page.locator("input[type='text']");
        assertTrue(inputField.isDisabled());

        page.locator("button:has-text('Enable')").click();

        Locator message = page.locator("p[id='message']");
        message.waitFor();
//Проверяем что поле активно после клика и ожидания
        assertTrue(inputField.isEnabled());
    }
    @AfterEach
    void tearDown(){
        try {
            if (page != null){
                page.close();
            }
        } catch (Exception e){
            // Игнор искоючения
        }

        try {
            if (browser != null){
                browser.close();
            }
        }catch (Exception e){
            //Игнор исключения
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
