import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusCodeInterceptionTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
        context = browser.newContext();
        page = context.newPage();

        // Перехват запроса к /status_codes/404
        context.route("**/status_codes/404", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setHeaders(Collections.singletonMap("Content-Type", "text/html"))
                    .setBody("<h3>Mocked Success Response</h3>")
            );
        });
    }

    @Test
    void testMockedStatusCode() {
        page.navigate("https://the-internet.herokuapp.com/status_codes");

        // Клик по ссылке "404"

        Response response = page.waitForResponse("**/status_codes/404", () ->{
            page.locator("text=404").click();
        });

        // Проверка мок-текста
        //Проверка статус кода
        Assertions.assertEquals(200, response.status());

        //проверка текста h3
        Locator mockedText = page.locator("h3");
        mockedText.waitFor();
        Assertions.assertEquals("Mocked Success Response", mockedText.textContent());

        //проверка url
        Assertions.assertTrue(page.url().contains("status_codes/404"));

        //Проверка отсутсвия оригинального текста
        Assertions.assertFalse(page.locator("text = This page returned a 404 status code.").isVisible());

    }

    @AfterEach
    void tearDown() {
        browser.close();
        playwright.close();
    }
}
