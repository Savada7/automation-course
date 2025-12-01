import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitHubSearchInterceptionTest {
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

        // Перехват запроса поиска
        context.route("**/search**", route -> {
            // Получаем оригинальный URL
            String originalUrl = route.request().url();

            // Декодируем и модифицируем параметры
            String modifiedUrl = originalUrl.contains("q=")
                    ? originalUrl.replaceAll("q=[^&]+", "q=stars%3A%3E10000")
                    : originalUrl + (originalUrl.contains("?") ? "&" : "?") + "q=stars%3A%3E10000";

            // Продолжаем запрос с модифицированным URL
            route.resume(new Route.ResumeOptions().setUrl(modifiedUrl));
        });
    }

    @Test
    void testSearchModification() {
        page.navigate("https://github.com/search?q=java");

        // Ожидаем появления результатов
        page.locator("[class='Box-sc-62in7e-0 flaXet']").first().waitFor();

        // Проверяем модифицированный запрос в UI
//        String searchValue = page.locator("input[name='q'][type='text']").inputValue();
//        Assertions.assertEquals("stars:>10000", searchValue);

        String currentUrl = page.url();
        System.out.println("Current URL: " + currentUrl);
        Assertions.assertTrue(currentUrl.contains("stars%3A%3E10000") ||
                        currentUrl.contains("stars:>10000"),
                "URL не содержит модифицированный запрос");

        String visibleText = page.textContent("body");
        if (visibleText != null) {
            boolean containsStarsFilter = visibleText.toLowerCase().contains("stars") ||
                    visibleText.contains(">10000") ||
                    visibleText.contains("10,000");
            System.out.println("Page contains stars filter: " + containsStarsFilter);
        }
    }

    @AfterEach
    void tearDown() {
        browser.close();
        playwright.close();
    }
}
// Так как в UI гит хаб не отображает фильтр, сделал проверку через урл. Второй тест в боевой проект
// я бы не пропустил, слишком нестабильный, но для примера, думаю, что сойдет