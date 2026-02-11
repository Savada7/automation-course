package com.qa.tests;

import com.microsoft.playwright.Page;
import com.qa.utils.PageFactory;
import com.qa.utils.PlaywrightManager;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseTest {
    protected PageFactory pageFactory;
    protected Page page;

    @BeforeEach
    public void setUp() {
        page = PlaywrightManager.createNewPage();
        pageFactory = new PageFactory(page);
    }

    @AfterEach
    public void tearDown() {
        if (page != null) {
            page.close();
        }
    }

    @AfterAll
    public static void tearDownAll() {
        PlaywrightManager.closeBrowser();
    }
}