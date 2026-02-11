package com.qa.utils;

import com.microsoft.playwright.*;

public class PlaywrightManager {
    private static Playwright playwright;
    private static Browser browser;

    public static Browser getBrowser() {
        if (browser == null) {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setSlowMo(100));
        }
        return browser;
    }

    public static Page createNewPage() {
        BrowserContext context = getBrowser().newContext();
        Page page = context.newPage();
        return page;
    }

    public static void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}