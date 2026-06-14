package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MockedApiTest {
    static Playwright playwright;
    static Browser browser;
    private BrowserContext context;
    private Page page;

    private static ApiService apiService;

    @BeforeAll
    static void setUpClass() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
        // Создаем мок ApiService
        apiService = mock(ApiService.class);

        // Настраиваем поведение мока - возвращаем тестовые данные
        when(apiService.fetchUserData()).thenReturn(
                "{\"name\": \"Test User\", \"email\": \"test@example.com\"}"
        );
    }

    @BeforeEach
    void setUp() {
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    void testUserProfileWithMockedApi() {
        // Используем мок вместо реального API
        String userData = apiService.fetchUserData();

        page.navigate("https://the-internet.herokuapp.com/dynamic_content");
        page.evaluate("(data) => { window.userData = data; }", userData);

        // Проверяем, что данные корректно обрабатываются
        Object result = page.evaluate("() => window.userData");
        assertNotNull(result);
        assertTrue(result.toString().contains("Test User"));
    }

    @Test
    void testWithDifferentMockData() {
        // Изменяем поведение мока для другого теста
        when(apiService.fetchUserData()).thenReturn(
                "{\"name\": \"Admin\", \"email\": \"admin@example.com\"}"
        );

        String userData = apiService.fetchUserData();
        assertTrue(userData.contains("Admin"));
    }

    // Внутренний класс, имитирующий медленный API
    static class ApiService {
        public String fetchUserData() {
            try {
                Thread.sleep(3000); // 3 секунды задержки
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "{\"name\": \"Real User\", \"email\": \"real@example.com\"}";
        }
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
    }

    @AfterAll
    static void tearDownClass() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
