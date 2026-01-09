package com.qa.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class DynamicLoadingTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @Test
    void testDynamicLoading(){
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        context = browser.newContext();

        //Трассировка
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();

        //page.navigate("https://the-internet.herokuapp.com/dynamic_loading/1");

        Response responseAfterNavigate = page.waitForResponse(
                r -> r.url().contains("dynamic_loading") && r.status() == 200,
                () -> {
                    page.navigate("https://the-internet.herokuapp.com/dynamic_loading/1");
                }
        );

        Assertions.assertEquals(200,responseAfterNavigate.status() );
        //на этот эндпоит после навигейта

        Response response = page.waitForResponse(
                r -> r.url().contains("ajax-loader") && r.status() == 200,
                () -> {
                    page.click("button");
                }
        );
        Assertions.assertEquals(200, response.status());
        //так как по нажатию на кнопку есть запрос только на лоадер сделал вроверку на него

        page.locator("#finish").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));

        String text = page.locator("#finish").textContent();
        Assertions.assertTrue(text.contains("Hello World!"));

        context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("trace-dynamic-loading-pz11.zip")));
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
