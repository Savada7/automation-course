package com.qa.utils;

import com.microsoft.playwright.*;
import com.qa.config.EnvConfig;
import org.aeonbits.owner.ConfigFactory;

public class PlaywrightManager {
    private static Playwright playwright;
    private static Browser browser;
    private static EnvConfig config;
    private static APIRequestContext apiRequestContext;

    static {
        config = ConfigFactory.create(EnvConfig.class, System.getProperties());
    }

    public static Browser getBrowser() {
        if (browser == null) {
            playwright = Playwright.create();
            BrowserType browserType = getBrowserType(playwright, config.browser());
            browser = browserType.launch(new BrowserType.LaunchOptions()
                    .setHeadless(config.headless())
                    .setSlowMo(100));
        }
        return browser;
    }

    public static APIRequestContext getApiContext() {
        if (apiRequestContext == null) {
            if (playwright == null) {
                playwright = Playwright.create();
            }
            apiRequestContext = playwright.request().newContext(
                    new APIRequest.NewContextOptions()
                            .setBaseURL(config.baseUrl().replaceAll("/$", ""))
            );
        }
        return apiRequestContext;
    }

    public static Page createNewPage() {
        BrowserContext context = getBrowser().newContext();
        return context.newPage();
    }

    public static void closeApiContext() {
        if (apiRequestContext != null) {
            apiRequestContext.dispose();
            apiRequestContext = null;
        }
    }

    private static BrowserType getBrowserType(Playwright playwright, String browserName) {
        switch (browserName.toLowerCase()) {
            case "firefox":
                return playwright.firefox();
            case "webkit":
                return playwright.webkit();
            default:
                return playwright.chromium();
        }
    }

    public static void closeBrowser() {
        closeApiContext();
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