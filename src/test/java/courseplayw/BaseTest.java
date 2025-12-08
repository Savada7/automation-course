package courseplayw;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ExtendWith(AllureJunit5.class)
public class BaseTest {
    // Директории для артефактов
    protected static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    protected static final Path TARGET_DIR = PROJECT_ROOT.resolve("target");
    protected static final Path ERRORS_DIR = TARGET_DIR.resolve("errors");
    protected static final Path VIDEOS_DIR = TARGET_DIR.resolve("videos");
    protected static final Path SCREENSHOTS_DIR = TARGET_DIR.resolve("screenshots");
    protected static final Path ALLURE_RESULTS_DIR = TARGET_DIR.resolve("allure-results");

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    private String testVideoPath;
    private String currentTestName;

    @BeforeAll
    static void setupAll() {
        System.out.println("=== Setting up test environment ===");

        // Создаем все необходимые директории
        createDirectories();

        // Создаем Playwright instance
        playwright = Playwright.create();

        // Настраиваем браузер (можно менять через системные свойства)
        String browserType = System.getProperty("browser", "chromium");
        boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "true"));

        System.out.println("Browser type: " + browserType);
        System.out.println("Headless mode: " + isHeadless);

        browser = switch (browserType.toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(
                    new BrowserType.LaunchOptions().setHeadless(isHeadless));
            case "webkit" -> playwright.webkit().launch(
                    new BrowserType.LaunchOptions().setHeadless(isHeadless));
            default -> playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(isHeadless)
                            .setArgs(java.util.List.of("--start-maximized")));
        };

        System.out.println("Browser initialized successfully");
    }

    @BeforeEach
    void setupTest(TestInfo testInfo) {
        currentTestName = testInfo.getDisplayName();
        Method testMethod = testInfo.getTestMethod().orElse(null);
        String methodName = testMethod != null ? testMethod.getName() : "unknown";

        System.out.println("\n=== Starting test: " + currentTestName + " ===");
        System.out.println("Method: " + methodName);

        // Создаем уникальное имя для видео
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String videoFileName = sanitizeFileName(methodName + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8));
        Path videoPath = VIDEOS_DIR.resolve(videoFileName + ".webm");

        // Создаем контекст с записью видео
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(VIDEOS_DIR)
                .setRecordVideoSize(1280, 720)
                .setViewportSize(1920, 1080));

        // Включаем запись видео
        context.setDefaultTimeout(30000);

        // Создаем страницу
        page = context.newPage();

        // Сохраняем путь к видео для прикрепления в Allure
        testVideoPath = videoPath.toString();

        // Логируем в Allure
        Allure.step("Setup test: " + currentTestName, () -> {
            Allure.addAttachment("Test Information", "text/plain",
                    "Test Name: " + currentTestName + "\n" +
                            "Method: " + methodName + "\n" +
                            "Browser: " + System.getProperty("browser", "chromium") + "\n" +
                            "Headless: " + System.getProperty("headless", "true") + "\n" +
                            "Video: " + videoFileName + ".webm");
        });
    }

    @AfterEach
    void tearDownTest(TestInfo testInfo) {
        System.out.println("\n=== Tearing down test: " + currentTestName + " ===");

        try {
            // Проверяем статус теста
            boolean testFailed = testInfo.getTags().contains("failed") ||
                    testInfo.getTestMethod()
                            .map(m -> m.isAnnotationPresent(Attachment.class))
                            .orElse(false);

            if (testFailed) {
                System.out.println("Test failed - capturing artifacts...");

                // 1. Сохраняем скриншот при ошибке
                saveScreenshot("error_" + currentTestName);

                // 2. Прикрепляем скриншот к Allure
                byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                Allure.addAttachment("Error Screenshot", "image/png",
                        new ByteArrayInputStream(screenshot), ".png");

                // 3. Сохраняем HTML страницы
                String pageSource = page.content();
                Allure.addAttachment("Page Source", "text/html",
                        new ByteArrayInputStream(pageSource.getBytes()), ".html");

                // 4. Сохраняем URL
                Allure.addAttachment("Current URL", "text/plain", page.url());

                System.out.println("Artifacts captured for failed test");
            }

            // 5. Прикрепляем видео к Allure (если запись велась)
            attachVideoToAllure();

        } catch (Exception e) {
            System.err.println("Error during test teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Всегда закрываем контекст
            if (context != null) {
                context.close();
                System.out.println("Browser context closed");
            }
        }
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("\n=== Cleaning up test environment ===");

        if (browser != null) {
            browser.close();
            System.out.println("Browser closed");
        }

        if (playwright != null) {
            playwright.close();
            System.out.println("Playwright closed");
        }

        System.out.println("=== Test environment cleanup complete ===");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Создание всех необходимых директорий
     */
    private static void createDirectories() {
        System.out.println("Creating directories...");

        String[] dirs = {
                TARGET_DIR.toString(),
                ERRORS_DIR.toString(),
                VIDEOS_DIR.toString(),
                SCREENSHOTS_DIR.toString(),
                ALLURE_RESULTS_DIR.toString()
        };

        for (String dir : dirs) {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                    System.out.println("Created directory: " + path.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Failed to create directory " + dir + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Сохранение скриншота
     */
    protected void saveScreenshot(String screenshotName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String fileName = sanitizeFileName(screenshotName + "_" + timestamp) + ".png";
            Path screenshotPath = SCREENSHOTS_DIR.resolve(fileName);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true));

            System.out.println("Screenshot saved: " + screenshotPath);

            // Прикрепляем к Allure
            byte[] screenshotBytes = Files.readAllBytes(screenshotPath);
            Allure.addAttachment("Screenshot: " + screenshotName, "image/png",
                    new ByteArrayInputStream(screenshotBytes), ".png");

        } catch (Exception e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }

    /**
     * Прикрепление видео к Allure отчету
     */
    private void attachVideoToAllure() {
        try {
            // Ждем завершения записи видео
            Thread.sleep(1000);

            // Ищем последний созданный видеофайл
            Files.list(VIDEOS_DIR)
                    .filter(path -> path.toString().endsWith(".webm"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .findFirst()
                    .ifPresent(videoPath -> {
                        try {
                            byte[] videoBytes = Files.readAllBytes(videoPath);
                            Allure.addAttachment("Test Execution Video", "video/webm",
                                    new ByteArrayInputStream(videoBytes), ".webm");
                            System.out.println("Video attached to Allure: " + videoPath.getFileName());
                        } catch (IOException e) {
                            System.err.println("Failed to read video file: " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            System.err.println("Failed to attach video to Allure: " + e.getMessage());
        }
    }

    /**
     * Метод для Allure шагов со скриншотом
     */
    @Step("{stepName}")
    protected void allureStepWithScreenshot(String stepName, Runnable action) {
        System.out.println("Step: " + stepName);

        try {
            action.run();

            // Делаем скриншот после успешного выполнения шага
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(false));

            Allure.addAttachment("Step: " + stepName, "image/png",
                    new ByteArrayInputStream(screenshot), ".png");

        } catch (Exception e) {
            // При ошибке сохраняем скриншот с ошибкой
            byte[] errorScreenshot = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true));

            Allure.addAttachment("Error in step: " + stepName, "image/png",
                    new ByteArrayInputStream(errorScreenshot), ".png");

            Allure.addAttachment("Error details", "text/plain",
                    "Step: " + stepName + "\n" +
                            "Error: " + e.getMessage() + "\n" +
                            "URL: " + page.url());

            throw e;
        }
    }

    /**
     * Навигация с логированием
     */
    protected void navigateTo(String url, String pageDescription) {
        allureStepWithScreenshot("Navigate to: " + pageDescription, () -> {
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Логируем информацию о странице
            Allure.addAttachment("Page Information", "text/plain",
                    "URL: " + url + "\n" +
                            "Description: " + pageDescription + "\n" +
                            "Title: " + page.title() + "\n" +
                            "Current URL: " + page.url());
        });
    }

    /**
     * Клик с ожиданием и логированием
     */
    protected void clickElement(String selector, String elementDescription) {
        allureStepWithScreenshot("Click: " + elementDescription, () -> {
            Locator element = page.locator(selector);
            element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            element.click();

            Allure.addAttachment("Clicked Element", "text/plain",
                    "Selector: " + selector + "\n" +
                            "Description: " + elementDescription);
        });
    }

    /**
     * Заполнение поля с логированием
     */
    protected void fillField(String selector, String value, String fieldDescription) {
        Allure.step("Fill field: " + fieldDescription, () -> {
            page.fill(selector, value);

            Allure.addAttachment("Filled Field", "text/plain",
                    "Selector: " + selector + "\n" +
                            "Value: " + value + "\n" +
                            "Description: " + fieldDescription);
        });
    }

    /**
     * Очистка имени файла от недопустимых символов
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_")
                .trim();
    }

    /**
     * Получение текущего времени для логов
     */
    protected String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    /**
     * Ожидание с логированием
     */
    protected void waitForTimeout(int milliseconds, String reason) {
        Allure.step("Wait: " + reason + " (" + milliseconds + "ms)", () -> {
            System.out.println(getCurrentTime() + " - Waiting " + milliseconds + "ms: " + reason);
            page.waitForTimeout(milliseconds);
        });
    }

    /**
     * Проверка видимости элемента
     */
    protected void assertElementVisible(String selector, String elementDescription) {
        Allure.step("Assert element visible: " + elementDescription, () -> {
            boolean isVisible = page.locator(selector).isVisible();
            Assertions.assertTrue(isVisible, elementDescription + " should be visible");

            Allure.addAttachment("Assertion Result", "text/plain",
                    "Element: " + elementDescription + "\n" +
                            "Selector: " + selector + "\n" +
                            "Is Visible: " + isVisible);
        });
    }

    /**
     * Метод для прикрепления текста к Allure
     */
    protected void attachText(String name, String content) {
        Allure.addAttachment(name, "text/plain", content);
    }

    /**
     * Метод для прикрепления JSON к Allure
     */
    protected void attachJson(String name, String jsonContent) {
        Allure.addAttachment(name, "application/json", jsonContent);
    }

    /**
     * Получение текущей страницы (для доступа в тестах)
     */
    protected Page getPage() {
        return page;
    }

    /**
     * Получение текущего контекста
     */
    protected BrowserContext getContext() {
        return context;
    }
}