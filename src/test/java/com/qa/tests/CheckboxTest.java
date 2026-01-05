package com.qa.tests;

import com.microsoft.playwright.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class CheckboxTest {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    private Locator checkbox1;
    private Locator checkbox2;

    @BeforeEach
    @Step("Инициализация браузера и контекста")
    void setUp(){
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        context = browser.newContext();
        page = context.newPage();

        checkbox1 = page.locator("#checkboxes input[type='checkbox']").first();
        checkbox2 = page.locator("#checkboxes input[type='checkbox']").last();
    }
    @Test
    @Story("Проверка работы чекбоксов")
    @DisplayName("Тестирование выбора/ снятия чекбоксов")
    @Severity(SeverityLevel.CRITICAL)
    void testCheckboxes(){
        try {
            navigateToCheckboxesPage();
            verifyInitialState();
            toggleCheckboxes();
            verifyToggledState();
        } catch (Throwable t) {
            // Делаем скриншот при падении теста
            takeScreenshotOnFailure();
            throw t;
        }
    }

    // Метод для создания скриншота
    private void takeScreenshotOnFailure() {
        try {
            byte[] screenshot = page.screenshot();
            Allure.addAttachment(
                    "Screenshot on failure",
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    "png"
            );
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    @Step("Переход на страницу /checkboxes")
    private void navigateToCheckboxesPage(){
        page.navigate("https://the-internet.herokuapp.com/checkboxes");
        assertTrue(page.isVisible("h3:has-text('Checkboxes')"));
    }
    @Step("Проверка начального состояния чекбоксов")
    private void verifyInitialState(){
        assertFalse(checkbox1.isChecked());
        assertTrue(checkbox2.isChecked());
    }

    @Step("Изменение состояния чекбоксов")
    private void toggleCheckboxes(){
        checkbox1.check();
        checkbox2.uncheck();
    }
    @Step("Проверка изменения состояния чекбоксов")
    private void verifyToggledState(){
        assertTrue(checkbox1.isChecked());
        assertFalse(checkbox2.isChecked());
    }

    @AfterEach
    @Step("Закрытие ресурсов")
    void tearDown(){
        context.close();
        browser.close();
        playwright.close();
    }

}
