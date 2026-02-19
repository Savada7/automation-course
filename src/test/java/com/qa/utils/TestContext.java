package com.qa.utils;
import com.microsoft.playwright.*;

public class TestContext {
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext browserContext;
    private final Page page;

    public TestContext(){
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        this.browserContext = browser.newContext();
        this.page = browserContext.newPage();
    }

    public Page getPage(){
        return page;
    }

    public void close(){
        if (page != null){
            page.close();
        }
        if (browserContext != null){
            browserContext.close();
        }
        if (browser != null){
            browser.close();
        }
        if (playwright != null){
            playwright.close();
        }
    }
}
