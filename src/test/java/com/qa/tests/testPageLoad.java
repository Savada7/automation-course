package com.qa.tests;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
public class testPageLoad {


    static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("chromium", "/"),
                Arguments.of("chromium", "/login"),
                Arguments.of("chromium", "/dropdown"),

                Arguments.of("firefox", "/"),
                Arguments.of("firefox", "/login"),
                Arguments.of("firefox", "/dropdown")
        );
    }

    @ParameterizedTest(name = "Тестирование страницы {1} в браузере {0}")
    @MethodSource("provideTestData")
    void testPageLoad(String browserType, String path) {
        try (Playwright playwright = Playwright.create()) {
            // Выбираем тип браузера
            BrowserType type;
            if ("firefox".equals(browserType)) {
                type = playwright.firefox();
            } else {
                type = playwright.chromium();
            }


            try (Browser browser = type.launch(new BrowserType.LaunchOptions().setHeadless(true))) {
                try (BrowserContext context = browser.newContext()) {
                    Page page = context.newPage();

                    try {
                        String fullUrl = "https://the-internet.herokuapp.com" + path;

                        page.navigate(fullUrl);

                        assertThat(page.locator("body")).isVisible();

                        String pageTitle = page.title();
                        System.out.println(browserType + " - " + path + ": " + pageTitle);

                    } finally {
                        page.close();
                    }
                }
            }
        }
    }
}
