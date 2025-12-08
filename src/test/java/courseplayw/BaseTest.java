package courseplayw;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
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

// Если есть проблема с AllureJunit5, можно убрать @ExtendWith
// @ExtendWith(AllureJunit5.class)
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
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss_SSS");

    private static String globalRunId;

    @BeforeAll
    static void setupAll() {
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("=== SETTING UP TEST ENVIRONMENT ===");

        // Создаем уникальный ID для этого запуска
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        globalRunId = "run_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Убедимся, что target директория существует
        if (!Files.exists(TARGET_DIR)) {
            createDirectory(TARGET_DIR);
        }

        // Создаем основную директорию с timestamp
        TIMESTAMP_DIR = TARGET_DIR.resolve(timestamp);

        // Создаем поддиректории
        ERRORS_DIR = TIMESTAMP_DIR.resolve("errors");
        VIDEOS_DIR = TIMESTAMP_DIR.resolve("videos");
        SCREENSHOTS_DIR = TIMESTAMP_DIR.resolve("screenshots");
        ALLURE_RESULTS_DIR = TIMESTAMP_DIR.resolve("allure-results");

        System.out.println("📅 Run ID: " + globalRunId);
        System.out.println("📁 Timestamp directory: " + TIMESTAMP_DIR.toAbsolutePath());

        // Создаем все директории
        createDirectories();

        // Создаем файл с информацией о запуске
        createRunInfoFile(globalRunId, timestamp);

        // Инициализируем Playwright
        playwright = Playwright.create();

        // Настраиваем браузер
        String browserType = System.getProperty("browser", "chromium");
        boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        int slowMo = Integer.parseInt(System.getProperty("slow.mo", "0"));

        System.out.println("🌐 Browser: " + browserType);
        System.out.println("👻 Headless: " + isHeadless);
        System.out.println("🐌 SlowMo: " + slowMo + "ms");

        BrowserType browserTypeInstance;
        switch (browserType.toLowerCase()) {
            case "firefox":
                browserTypeInstance = playwright.firefox();
                break;
            case "webkit":
                browserTypeInstance = playwright.webkit();
                break;
            case "chromium":
            default:
                browserTypeInstance = playwright.chromium();
        }

        browser = browserTypeInstance.launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(isHeadless)
                        .setSlowMo(slowMo)
                        .setArgs(java.util.List.of(
                                "--start-maximized",
                                "--disable-dev-shm-usage",
                                "--no-sandbox"
                        )));

        System.out.println("✅ Test environment setup complete");
        System.out.println("══════════════════════════════════════════════════\n");
    }

    @BeforeEach
    void setupTest(TestInfo testInfo) {
        currentTestName = testInfo.getDisplayName();
        Method testMethod = testInfo.getTestMethod().orElse(null);
        String methodName = testMethod != null ? testMethod.getName() : "unknown";

        // Генерируем ID для теста
        testRunId = sanitizeFileName(methodName) + "_" + System.currentTimeMillis();

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("▶ STARTING TEST: " + currentTestName);
        System.out.println("   Method: " + methodName);
        System.out.println("   Test ID: " + testRunId);

        // Создаем директорию для видео этого теста
        Path testVideoDir = VIDEOS_DIR.resolve(testRunId);
        createDirectory(testVideoDir);

        // Создаем контекст с записью видео
        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(testVideoDir)
                .setRecordVideoSize(1280, 720)
                .setViewportSize(1920, 1080)
                .setPermissions(java.util.List.of("clipboard-read", "clipboard-write")));

        // Создаем страницу
        page = context.newPage();
        page.setDefaultTimeout(30000);

        // Включаем логирование
        page.onConsoleMessage(msg -> {
            System.out.println("📝 Console: " + msg.text());
        });

        page.onPageError(error -> {
            System.err.println("❌ Page error: " + error);
        });

        // Логируем в Allure если доступно
        try {
            Allure.step("Setup test: " + currentTestName, () -> {
                Allure.addAttachment("Test Information", "text/plain",
                        "Test Name: " + currentTestName + "\n" +
                                "Method: " + methodName + "\n" +
                                "Test ID: " + testRunId + "\n" +
                                "Run ID: " + globalRunId + "\n" +
                                "Browser: " + System.getProperty("browser", "chromium") + "\n" +
                                "Video Directory: " + testVideoDir.getFileName());
            });
        } catch (Exception e) {
            System.out.println("⚠ Allure not available: " + e.getMessage());
        }

        System.out.println("✅ Test setup complete");
    }

    @AfterEach
    void tearDownTest(TestInfo testInfo) {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("◀ TEARING DOWN TEST: " + currentTestName);

        try {
            // Проверяем статус теста
            boolean testFailed = testInfo.getTags().contains("failed");

            if (testInfo.getTestMethod().isPresent()) {
                Method method = testInfo.getTestMethod().get();
                if (method.isAnnotationPresent(Attachment.class)) {
                    testFailed = true;
                }
            }

            // Создаем директорию для скриншотов этого теста
            Path testScreenshotDir = SCREENSHOTS_DIR.resolve(testRunId);
            createDirectory(testScreenshotDir);

            // Сохраняем финальный скриншот
            saveScreenshot(testScreenshotDir, "final_state");

            if (testFailed) {
                System.out.println("❌ Test failed - capturing error artifacts...");

                // Сохраняем дополнительные скриншоты при ошибке
                saveScreenshot(testScreenshotDir, "error_final");

                try {
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
                } catch (Exception e) {
                    System.out.println("⚠ Could not attach to Allure: " + e.getMessage());
                }

                // Сохраняем HTML локально
                saveHtmlPage(testScreenshotDir, "error_page");

                System.out.println("📁 Error artifacts saved to: " + testScreenshotDir);
            } else {
                System.out.println("✅ Test passed");
            }

            // Прикрепляем видео к Allure
            attachVideoToAllure(testRunId);

        } catch (Exception e) {
            System.err.println("⚠ Error during test teardown: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Всегда закрываем контекст
            if (context != null) {
                context.close();
                System.out.println("🔒 Browser context closed");
            }
        }

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("══════════════════════════════════════════════════");
        System.out.println("=== CLEANING UP TEST ENVIRONMENT ===");

        // Создаем summary файл
        createRunSummaryFile();

        if (browser != null) {
            browser.close();
            System.out.println("🔒 Browser closed");
        }

        if (playwright != null) {
            playwright.close();
            System.out.println("🔒 Playwright closed");
        }

        System.out.println("📁 All artifacts saved in: " + TIMESTAMP_DIR.toAbsolutePath());
        System.out.println("==================================================");

        // Выводим структуру директории
        printDirectoryStructure();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Создание всех необходимых директорий
     */
    private static void createDirectories() {
        System.out.println("📂 Creating directories...");

        Path[] dirs = {TIMESTAMP_DIR, ERRORS_DIR, VIDEOS_DIR, SCREENSHOTS_DIR, ALLURE_RESULTS_DIR};

        for (Path dir : dirs) {
            createDirectory(dir);
        }

        System.out.println("✅ Directories created successfully");
    }

    /**
     * Создание одной директории
     */
    private static void createDirectory(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("   ✓ Created: " + path.toAbsolutePath());
            } else {
                System.out.println("   ✓ Already exists: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("   ✗ Failed to create directory " + path + ": " + e.getMessage());
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
        props.setProperty("os.name", System.getProperty("os.name", "Unknown"));
        props.setProperty("os.version", System.getProperty("os.version", "Unknown"));
        props.setProperty("os.arch", System.getProperty("os.arch", "Unknown"));
        props.setProperty("java.version", System.getProperty("java.version", "Unknown"));
        props.setProperty("java.vendor", System.getProperty("java.vendor", "Unknown"));
        props.setProperty("user.name", System.getProperty("user.name", "Unknown"));
        props.setProperty("user.dir", System.getProperty("user.dir", "Unknown"));
        props.setProperty("browser", System.getProperty("browser", "chromium"));
        props.setProperty("headless", System.getProperty("headless", "true"));
        props.setProperty("slow.mo", System.getProperty("slow.mo", "0"));

        try (FileWriter writer = new FileWriter(runInfoFile.toFile())) {
            props.store(writer, "Test Run Information");
            System.out.println("📝 Run info file created: " + runInfoFile.getFileName());
        } catch (IOException e) {
            System.err.println("✗ Failed to create run info file: " + e.getMessage());
        }
    }

    /**
     * Создание summary файла
     */
    private static void createRunSummaryFile() {
        Path summaryFile = TIMESTAMP_DIR.resolve("RUN_SUMMARY.md");

        try (FileWriter writer = new FileWriter(summaryFile.toFile())) {
            writer.write("# Test Run Summary\n\n");
            writer.write("## 📋 Run Information\n");
            writer.write("- **Run ID:** " + globalRunId + "\n");
            writer.write("- **Timestamp:** " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n");
            writer.write("- **Directory:** " + TIMESTAMP_DIR.getFileName() + "\n");
            writer.write("- **OS:** " + System.getProperty("os.name", "Unknown") + "\n");
            writer.write("- **Java Version:** " + System.getProperty("java.version", "Unknown") + "\n");
            writer.write("- **Browser:** " + System.getProperty("browser", "chromium") + "\n");
            writer.write("- **Headless:** " + System.getProperty("headless", "true") + "\n");
            writer.write("- **Slow Motion:** " + System.getProperty("slow.mo", "0") + "ms\n");

            writer.write("\n## 📁 Directory Structure\n");
            writer.write("```\n");
            writer.write(TIMESTAMP_DIR.toAbsolutePath() + "\n");
            writer.write("├── errors/           # Error screenshots and logs\n");
            writer.write("├── videos/           # Video recordings (.webm)\n");
            writer.write("│   ├── test1_xxx/    # Videos for test 1\n");
            writer.write("│   └── test2_xxx/    # Videos for test 2\n");
            writer.write("├── screenshots/      # Test screenshots (.png)\n");
            writer.write("│   ├── test1_xxx/    # Screenshots for test 1\n");
            writer.write("│   └── test2_xxx/    # Screenshots for test 2\n");
            writer.write("├── allure-results/   # Allure results (.json)\n");
            writer.write("├── run-info.properties\n");
            writer.write("└── RUN_SUMMARY.md\n");
            writer.write("```\n");

            writer.write("\n## 🚀 How to Use\n");
            writer.write("1. Open screenshots/ to view test screenshots\n");
            writer.write("2. Open videos/ to watch test recordings\n");
            writer.write("3. For Allure report: `allure serve " + TIMESTAMP_DIR.getFileName() + "/allure-results`\n");

            System.out.println("📝 Run summary created: " + summaryFile.getFileName());
        } catch (IOException e) {
            System.err.println("✗ Failed to create run summary: " + e.getMessage());
        }
    }

    /**
     * Сохранение скриншота
     */
    protected void saveScreenshot(Path screenshotDir, String screenshotName) {
        try {
            String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
            String fileName = String.format("%s_%s.png",
                    sanitizeFileName(screenshotName), timestamp);

            Path screenshotPath = screenshotDir.resolve(fileName);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(true));

            System.out.println("   📸 Screenshot saved: " + screenshotPath.getFileName());

            // Также прикрепляем к Allure если доступно
            try {
                byte[] screenshotBytes = Files.readAllBytes(screenshotPath);
                Allure.addAttachment("Screenshot: " + screenshotName, "image/png",
                        new ByteArrayInputStream(screenshotBytes), ".png");
            } catch (Exception e) {
                // Игнорируем если Allure не доступен
            }

        } catch (Exception e) {
            System.err.println("✗ Failed to save screenshot: " + e.getMessage());
        }
    }

    /**
     * Сохранение HTML страницы
     */
    private void saveHtmlPage(Path directory, String fileName) {
        try {
            String htmlContent = page.content();
            String safeFileName = sanitizeFileName(fileName) + ".html";
            Path htmlPath = directory.resolve(safeFileName);

            Files.writeString(htmlPath, htmlContent);
            System.out.println("   🌐 HTML page saved: " + htmlPath.getFileName());

        } catch (Exception e) {
            System.err.println("✗ Failed to save HTML page: " + e.getMessage());
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
                                System.out.println("   🎬 Video attached to Allure: " + videoPath.getFileName());
                            } catch (IOException e) {
                                System.err.println("✗ Failed to read video file: " + e.getMessage());
                            }
                        });
            }

        } catch (Exception e) {
            System.err.println("✗ Failed to attach video to Allure: " + e.getMessage());
        }
    }

    /**
     * Вывод структуры директории
     */
    private static void printDirectoryStructure() {
        try {
            System.out.println("\n📁 DIRECTORY STRUCTURE:");
            Files.walk(TIMESTAMP_DIR, 3)
                    .forEach(path -> {
                        try {
                            int depth = TIMESTAMP_DIR.relativize(path).getNameCount();
                            String indent = "  ".repeat(depth);
                            String prefix = Files.isDirectory(path) ? "📁 " : "📄 ";

                            if (depth == 0) {
                                System.out.println("📁 " + path.getFileName());
                            } else if (depth <= 2) {
                                System.out.println(indent + prefix + path.getFileName());
                            }
                        } catch (Exception e) {
                            // Игнорируем
                        }
                    });
        } catch (Exception e) {
            System.err.println("✗ Could not print directory structure: " + e.getMessage());
        }
    }

    /**
     * Очистка имени файла
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unnamed";
        }
        return name.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_+|_+$", "");
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ====================

    /**
     * Утилита для удобного сохранения скриншотов
     */
    protected void captureScreenshot(String stepName) {
        Path testScreenshotDir = SCREENSHOTS_DIR.resolve(testRunId);
        createDirectory(testScreenshotDir);
        saveScreenshot(testScreenshotDir, stepName);
    }

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
        System.out.println("🌐 Navigating to: " + pageDescription);

        try {
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
        } catch (Exception e) {
            // Без Allure
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            captureScreenshot("navigate_" + sanitizeFileName(pageDescription));
        }
    }

    /**
     * Клик с логированием и скриншотом
     */
    protected void clickWithScreenshot(String selector, String elementDescription) {
        System.out.println("🖱️ Clicking: " + elementDescription);

        try {
            Allure.step("Click: " + elementDescription, () -> {
                page.click(selector);
                captureScreenshot("click_" + sanitizeFileName(elementDescription));

                Allure.addAttachment("Clicked Element", "text/plain",
                        "Selector: " + selector + "\n" +
                                "Description: " + elementDescription);
            });
        } catch (Exception e) {
            // Без Allure
            page.click(selector);
            captureScreenshot("click_" + sanitizeFileName(elementDescription));
        }
    }

    /**
     * Заполнение поля с логированием
     */
    protected void fillWithLog(String selector, String value, String fieldDescription) {
        System.out.println("⌨️ Filling: " + fieldDescription + " = '" + value + "'");

        try {
            Allure.step("Fill: " + fieldDescription, () -> {
                page.fill(selector, value);

                Allure.addAttachment("Filled Field", "text/plain",
                        "Selector: " + selector + "\n" +
                                "Value: " + value + "\n" +
                                "Description: " + fieldDescription);
            });
        } catch (Exception e) {
            // Без Allure
            page.fill(selector, value);
        }
    }

    /**
     * Ожидание с логированием
     */
    protected void waitForTimeout(int milliseconds, String reason) {
        System.out.println("⏳ Waiting " + milliseconds + "ms: " + reason);

        try {
            Allure.step("Wait: " + reason + " (" + milliseconds + "ms)", () -> {
                page.waitForTimeout(milliseconds);
            });
        } catch (Exception e) {
            // Без Allure
            page.waitForTimeout(milliseconds);
        }
    }

    /**
     * Получить текущую страницу
     */
    protected Page getPage() {
        return page;
    }

    /**
     * Получить ID текущего теста
     */
    protected String getTestRunId() {
        return testRunId;
    }

    /**
     * Получить глобальный ID запуска
     */
    protected static String getGlobalRunId() {
        return globalRunId;
    }
}