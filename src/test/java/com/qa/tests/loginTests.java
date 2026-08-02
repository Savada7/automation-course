package com.qa.tests;

import com.microsoft.playwright.*;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class loginTests {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeEach
    @Step("Инициализация браузера и контекста")
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    @DisplayName("Тест логина с проверкой производительности")
    void loginTest() {
        // включаем трассировку
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(false));

        long startTime = System.currentTimeMillis();

        page.navigate("https://the-internet.herokuapp.com/login");
        page.locator("#username").fill("tomsmith");
        page.locator("#password").fill("SuperSecretPassword!");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/secure");

        String welcomeText = page.locator("h2").textContent();
        assertEquals(" Secure Area", welcomeText, "Авторизация не выполнена");

        long duration = System.currentTimeMillis() - startTime;

        saveExecutionTime(duration);

        // Проверка времени
        if (duration >= 3000) {
            // сохраняем трассировку
            context.tracing().stop(new Tracing.StopOptions()
                    .setPath(Paths.get("slow-login-trace.zip")));
            fail(String.format("Время выполнения входа %d мс превышает максимально допустимое время 3000 мс", duration));
        } else {
            // Если тест успешен - просто останавливаем трассировку без сохранения
            context.tracing().stop();
        }
    }

    /**
     * Сохраняет время выполнения в отчет Allure как аттачмент
     */
    @Attachment(value = "Время выполнения входа", type = "text/plain")
    private String saveExecutionTime(long duration) {
        return String.format("""
            Время выполнения: %d мс (%.2f сек)
            Лимит:           3000 мс (3.00 сек)
            Статус:          %s
            """,
                duration,
                duration / 1000.0,
                duration < 3000 ? "УСПЕШНО" : "ПРЕВЫШЕНИЕ ЛИМИТА"
        );
    }
}