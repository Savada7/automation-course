package com.qa.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DynamicControlTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    Locator checkBox;

    @BeforeEach
    void setUp(){
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        context = browser.newContext();
        page = context.newPage();

        checkBox = page.locator("input[type='checkbox']");
    }

    @Test
    void testDynamicCheckbox(){
        page.navigate("https://the-internet.herokuapp.com/dynamic_controls");

        Assertions.assertTrue(checkBox.isVisible());

        Locator removeBtn = page.locator("button:has-text('Remove')");
        removeBtn.click();

        checkBox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        Locator message = page.locator("p#message");
        Assertions.assertEquals("It's gone!", message.textContent());

        Locator addBtn = page.locator("button:has-text('Add')");
        addBtn.click();

        checkBox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Assertions.assertTrue(checkBox.isVisible());
    }

    @AfterEach
    void tearDown(){
        page.close();
        context.close();
        browser.close();
        playwright.close();
    }
}
