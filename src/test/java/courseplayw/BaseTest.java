package courseplayw;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Allure;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.UUID;

@ExtendWith(AllureJunit5.class)
public class BaseTest {
    // Базовые директории
    protected static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    protected static final Path TARGET_DIR = PROJECT_ROOT.resolve("target");

    // Директория с timestamp для текущего запуска
    protected static Path TIMESTAMP_DIR;
    protected static Path ERRORS_DIR;
    protected static Path VIDEOS_DIR;
    protected static Path SCREENSHOTS_DIR;
    protected static Path ALLURE_RESULTS_DIR;

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    private String currentTestName;
    private String testRunId;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @BeforeAll
    static void setupAll() {
        System.out.println("=== Setting up test environment ===");

        // Создаем уникальный ID для этого запуска
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String runId = "run_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Создаем основную директорию с timestamp
        TIMESTAMP_DIR = TARGET_DIR.resolve(timestamp);

        // Создаем поддиректории
        ERRORS_DIR = TIMESTAMP_DIR.resolve("errors");
        VIDEOS_DIR = TIMESTAMP_DIR.resolve("videos");
        SCREENSHOTS_DIR = TIMESTAMP_DIR.resolve("screenshots");
        ALLURE_RESULTS_DIR = TIMESTAMP_DIR.resolve("allure-results");

        System.out.println("Run ID: " + runId);
        System.out.println("Timestamp directory: " + TIMESTAMP_DIR.toAbsolutePath());

        // Создаем все директории
        createDirectories();

        // Создаем файл с информацией о запуске
        createRunInfoFile(runId, timestamp);

        // Инициализируем Playwright
        playwright = Playwright.create();

        // Настраиваем браузер
        String browserType = System.getProperty("browser", "chromium");
        boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        int slowMo = Integer.parseInt(System.getProperty("slow.mo", "0"));

        System.out.println("Browser: " + browserType + ", Headless: " + isHeadless + ", SlowMo: " + slowMo);

        browser = switch (browserType.toLowerCase()) {
            case "firefox" -> playwright.firefox().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(isHeadless)
                            .setSlowMo(slowMo));
            case "webkit" -> playwright.webkit().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(isHeadless)
                            .setSlowMo(slowMo));
            default -> playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(isHeadless)
                            .setSlowMo(slowMo)
                            .setArgs(java.util.List.of("--start-maximized")));
        };

        System.out.println("=== Test environment setup complete ===");
    }

    @BeforeEach
    void setupTest(TestInfo testInfo) {
        currentTestName = testInfo.getDisplayName();
        Method testMethod = testInfo.getTestMethod().orElse(null);
        String methodName = testMethod != null ? testMethod.getName() : "unknown";

        // Генерируем ID для теста
        testRunId = methodName + "_" + System.currentTimeMillis();

        System.out.println("\n--- Starting test: " + currentTestName + " ---");
        System.out.println("Test ID: " + testRunId);

        // Создаем директорию для видео этого теста
        Path testVideoDir = VIDEOS_DIR.resolve(testRunId);
        createDirectory(testVideoDir);

        // Создаем контекст с записью видео
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(testVideoDir)  // Видео в поддиректории теста
                .setRecordVideoSize(1280, 720)
                .setViewportSize(1920, 1080));

        // Создаем страницу
        page = context.newPage();
        page.setDefaultTimeout(30000);

        // Логируем в Allure
        Allure.step("Setup test: " + currentTestName, () -> {
            Allure.addAttachment("Test Information", "text/plain",
                    "Test Name: " + currentTestName + "\n" +
                            "Method: " + methodName + "\n" +
                            "Test ID: " + testRunId + "\n" +
                            "Browser: " + System.getProperty("browser", "chromium") + "\n" +
                            "Run Directory: " + TIMESTAMP_DIR.getFileName() + "\n" +
                            "Video Directory: " + testVideoDir.getFileName());
        });
    }

    @AfterEach
    void tearDownTest(TestInfo testInfo) {
        System.out.println("\n--- Tearing down test: " + currentTestName + " ---");

        try {
            // Проверяем статус теста
            boolean testFailed = testInfo.getTags().contains("failed") ||
                    (testInfo.getTestMethod().isPresent() &&
                            testInfo.getTestMethod().get().isAnnotationPresent(io.qameta.allure.Attachment.class));

            // Создаем директорию для скриншотов этого теста
            Path testScreenshotDir = SCREENSHOTS_DIR.resolve(testRunId);
            createDirectory(testScreenshotDir);

            // Сохраняем финальный скриншот
            saveScreenshot(testScreenshotDir, "final_state");

            if (testFailed) {
                System.out.println("Test failed - capturing error artifacts...");

                // Сохраняем дополнительные скриншоты при ошибке
                saveScreenshot(testScreenshotDir, "error_final");

                // Прикрепляем скриншот к Allure
                byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                Allure.addAttachment("Error Screenshot - " + currentTestName, "image/png",
                        new ByteArrayInputStream(screenshot), ".png");

                // Сохраняем HTML страницы
                String pageSource = page.content();
                Allure.addAttachment("Page Source - " + currentTestName, "text/html",
                        new ByteArrayInputStream(pageSource.getBytes()), ".html");

                // Сохраняем URL
                Allure.addAttachment("Current URL - " + currentTestName, "text/plain", page.url());

                // Сохраняем HTML локально
                saveHtmlPage(testScreenshotDir, "error_page");

                System.out.println("Error artifacts saved to: " + testScreenshotDir);
            }

            // Прикрепляем видео к Allure
            attachVideoToAllure(testRunId);

        } catch (Exception e) {
            System.err.println("Error during test teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Всегда закрываем контекст
            if (context != null) {
                context.close();
                System.out.println("Browser context closed for test: " + currentTestName);
            }
        }
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("\n=== Cleaning up test environment ===");

        // Создаем summary файл
        createRunSummaryFile();

        if (browser != null) {
            browser.close();
            System.out.println("Browser closed");
        }

        if (playwright != null) {
            playwright.close();
            System.out.println("Playwright closed");
        }

        System.out.println("Artifacts saved in: " + TIMESTAMP_DIR.toAbsolutePath());
        System.out.println("=== Test environment cleanup complete ===");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Создание всех необходимых директорий
     */
    private static void createDirectories() {
        System.out.println("Creating directories...");

        Path[] dirs = {TIMESTAMP_DIR, ERRORS_DIR, VIDEOS_DIR, SCREENSHOTS_DIR, ALLURE_RESULTS_DIR};

        for (Path dir : dirs) {
            createDirectory(dir);
        }
    }

    /**
     * Создание одной директории
     */
    private static void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Created directory: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create directory " + path + ": " + e.getMessage());
        }
    }

    /**
     * Создание файла с информацией о запуске
     */
    private static void createRunInfoFile(String runId, String timestamp) {
        Path runInfoFile = TIMESTAMP_DIR.resolve("run-info.properties");

        Properties props = new Properties();
        props.setProperty("run.id", runId);
        props.setProperty("timestamp", timestamp);
        props.setProperty("os.name", System.getProperty("os.name"));
        props.setProperty("os.version", System.getProperty("os.version"));
        props.setProperty("java.version", System.getProperty("java.version"));
        props.setProperty("user.name", System.getProperty("user.name"));
        props.setProperty("browser", System.getProperty("browser", "chromium"));
        props.setProperty("headless", System.getProperty("headless", "true"));
        props.setProperty("slow.mo", System.getProperty("slow.mo", "0"));

        try (FileWriter writer = new FileWriter(runInfoFile.toFile())) {
            props.store(writer, "Test Run Information");
            System.out.println("Run info file created: " + runInfoFile);
        } catch (IOException e) {
            System.err.println("Failed to create run info file: " + e.getMessage());
        }
    }

    /**
     * Создание summary файла
     */
    private static void createRunSummaryFile() {
        Path summaryFile = TIMESTAMP_DIR.resolve("RUN_SUMMARY.md");

        try (FileWriter writer = new FileWriter(summaryFile.toFile())) {
            writer.write("# Test Run Summary\n\n");
            writer.write("## Run Information\n");
            writer.write("- **Timestamp:** " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n");
            writer.write("- **Directory:** " + TIMESTAMP_DIR.getFileName() + "\n");
            writer.write("- **OS:** " + System.getProperty("os.name") + "\n");
            writer.write("- **Java Version:** " + System.getProperty("java.version") + "\n");
            writer.write("- **Browser:** " + System.getProperty("browser", "chromium") + "\n");
            writer.write("- **Headless:** " + System.getProperty("headless", "true") + "\n");
            writer.write("\n## Directory Structure\n");
            writer.write("```\n");
            writer.write(TIMESTAMP_DIR.toAbsolutePath() + "\n");
            writer.write("├── errors/           # Error screenshots and logs\n");
            writer.write("├── videos/           # Video recordings\n");
            writer.write("│   ├── test1_xxx/    # Videos for test 1\n");
            writer.write("│   └── test2_xxx/    # Videos for test 2\n");
            writer.write("├── screenshots/      # Test screenshots\n");
            writer.write("│   ├── test1_xxx/    # Screenshots for test 1\n");
            writer.write("│   └── test2_xxx/    # Screenshots for test 2\n");
            writer.write("├── allure-results/   # Allure results\n");
            writer.write("├── run-info.properties\n");
            writer.write("└── RUN_SUMMARY.md\n");
            writer.write("```\n");

            System.out.println("Run summary created: " + summaryFile);
        } catch (IOException e) {
            System.err.println("Failed to create run summary: " + e.getMessage());
        }
    }

    /**
     * Сохранение скриншота
     */
    protected void saveScreenshot(Path screenshotDir, String screenshotName) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss_SSS"));
            String fileName = String.format("%s_%s.png",
                    sanitizeFileName(screenshotName), timestamp);

            Path screenshotPath = screenshotDir.resolve(fileName);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true));

            System.out.println("Screenshot saved: " + screenshotPath.getFileName());

        } catch (Exception e) {
            System.err.println("Failed to save screenshot: " + e.getMessage());
        }
    }

    /**
     * Сохранение HTML страницы
     */
    private void saveHtmlPage(Path directory, String fileName) {
        try {
            String htmlContent = page.content();
            Path htmlPath = directory.resolve(fileName + ".html");

            Files.writeString(htmlPath, htmlContent);
            System.out.println("HTML page saved: " + htmlPath.getFileName());

        } catch (Exception e) {
            System.err.println("Failed to save HTML page: " + e.getMessage());
        }
    }

    /**
     * Прикрепление видео к Allure
     */
    private void attachVideoToAllure(String testId) {
        try {
            Path testVideoDir = VIDEOS_DIR.resolve(testId);

            if (Files.exists(testVideoDir)) {
                Files.list(testVideoDir)
                        .filter(path -> path.toString().endsWith(".webm"))
                        .findFirst()
                        .ifPresent(videoPath -> {
                            try {
                                byte[] videoBytes = Files.readAllBytes(videoPath);
                                Allure.addAttachment("Video - " + currentTestName, "video/webm",
                                        new ByteArrayInputStream(videoBytes), ".webm");
                                System.out.println("Video attached to Allure: " + videoPath.getFileName());
                            } catch (IOException e) {
                                System.err.println("Failed to read video file: " + e.getMessage());
                            }
                        });
            }

        } catch (Exception e) {
            System.err.println("Failed to attach video to Allure: " + e.getMessage());
        }
    }

    /**
     * Утилита для удобного сохранения скриншотов
     */
    protected void captureScreenshot(String stepName) {
        Path testScreenshotDir = SCREENSHOTS_DIR.resolve(testRunId);
        createDirectory(testScreenshotDir);

        saveScreenshot(testScreenshotDir, stepName);

        // Также прикрепляем к Allure
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Allure.addAttachment("Screenshot: " + stepName, "image/png",
                new ByteArrayInputStream(screenshot), ".png");
    }

    /**
     * Очистка имени файла
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_")
                .trim();
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ====================

    /**
     * Получить путь к директории текущего запуска
     */
    public static Path getTimestampDir() {
        return TIMESTAMP_DIR;
    }

    /**
     * Получить путь к директории скриншотов
     */
    public static Path getScreenshotsDir() {
        return SCREENSHOTS_DIR;
    }

    /**
     * Получить путь к директории видео
     */
    public static Path getVideosDir() {
        return VIDEOS_DIR;
    }

    /**
     * Получить текущий timestamp
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    /**
     * Метод для перехода с сохранением скриншота
     */
    protected void navigateWithScreenshot(String url, String pageDescription) {
        Allure.step("Navigate to: " + pageDescription, () -> {
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Сохраняем скриншот
            captureScreenshot("navigate_" + sanitizeFileName(pageDescription));

            Allure.addAttachment("Page Info", "text/plain",
                    "URL: " + url + "\n" +
                            "Title: " + page.title() + "\n" +
                            "Description: " + pageDescription);
        });
    }
}