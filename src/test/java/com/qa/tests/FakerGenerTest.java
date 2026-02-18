package com.qa.tests;

import com.github.javafaker.Faker;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FakerGenerTest {

    @Test
    public void testDynamicContentWithMock() {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            context = browser.newContext();
            page = context.newPage();

            // Генерация случайного имени
            Faker faker = new Faker();
            String randomName = faker.name().fullName();

            // Мокирование API
            page.route("**/dynamic_content", route -> {
                String mockResponse = String.format(
                        "[{\"id\":1,\"content\":\"%s\"}]",
                        randomName
                );

                route.fulfill(new Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("application/json")
                        .setBody(mockResponse)
                );
            });

            page.navigate("https://the-internet.herokuapp.com/dynamic_content");

            // Ждем загрузки страницы
            page.waitForLoadState();

            // Проверяем наличие текста
            boolean nameFound = page.locator("text=" + randomName).isVisible();


            assertTrue(nameFound, "Имя должно отображаться на странице");

            page.waitForTimeout(2000);

        } catch (Exception e) {
            System.err.println("Ошибка в тесте: " + e.getMessage());
            e.printStackTrace();
            throw e;

        } finally {
            if (page != null) {
                page.close();
            }
            if (context != null) {
                context.close();
            }
            if (browser != null) {
                browser.close();
            }
            if (playwright != null) {
                playwright.close();
            }
        }
    }
}