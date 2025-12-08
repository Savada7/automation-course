package courseplayw;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class BaseTest {
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path ERRORS_DIR = PROJECT_ROOT.resolve("errors");
    private static final Path VIDEOS_DIR = PROJECT_ROOT.resolve("videos");

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        createDirectory(ERRORS_DIR);
        createDirectory(VIDEOS_DIR);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(false)
        );
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(VIDEOS_DIR)
                .setRecordVideoSize(1280, 720));

        page = context.newPage();
        page.setViewportSize(1920, 1080);
    }

    @AfterEach
    void closeContext(TestInfo testInfo) {
        try {
            // 1. Сохраняем скриншот ПЕРЕД закрытием контекста
            if (testInfo.getTags().contains("failed")) {
                takeScreenshot(testInfo.getDisplayName());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении скриншота: " + e.getMessage());
        } finally {
            // 2. Закрываем контекст только после попытки сохранения скриншота
            context.close();
        }
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    // Метод для ручного вызова при ошибках в тестах
    protected void captureScreenshot(String name) {
        takeScreenshot(name);
    }

    private void takeScreenshot(String testName) {
        try {
            String fileName = sanitizeFileName(testName + "_" + Instant.now().toEpochMilli()) + ".png";
            Path screenshotPath = ERRORS_DIR.resolve(fileName);

            System.out.println("Сохранение скриншота: " + screenshotPath);

            // Упрощенный скриншот без дополнительных проверок
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true));

            System.out.println("Скриншот успешно сохранен!");
        } catch (Exception e) {
            System.err.println("Финальная ошибка сохранения: " + e.getMessage());
        }
    }

    private static void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception ignored) {}
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}