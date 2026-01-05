package com.qa.tests;


import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.microsoft.playwright.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.microsoft.playwright.options.WaitUntilState;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Тесты для the-internet.herokuapp.com")
@Feature("Работа с JavaScript-алертами")
public class AdvancedReportingTest {
    private static ExtentReports extent;
    private Browser browser;
    private Playwright playwright;
    private Page page;
    private ExtentTest test;

    @BeforeAll
    static void setupExtent() {
        ExtentSparkReporter reporter = new ExtentSparkReporter("target/extent-report.html");
        reporter.config().setDocumentTitle("Playwright Extent Report");
        extent = new ExtentReports();
        extent.attachReporter(reporter);
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false));
        BrowserContext context = browser.newContext();
        page = context.newPage();

        test = extent.createTest(testInfo.getDisplayName());
    }

    @Test
    @Story("Проверка JS Alert")
    @Description("Тест взаимодействия с JS Alert и проверка результата")
    @Severity(SeverityLevel.NORMAL)
    void testJavaScriptAlert() {
        try {
            navigateToAlertsPage();
            String alertMessage = foJsAlert();
            verifyResultText();
            captureSuccessScreenshot();

            logExtent(Status.PASS, "Тест успешно завершен с сообщением: " + alertMessage);

        } catch (Exception e) {
            foTestFailure(e);
            throw e;
        }
    }

    @Step("Открыть страницу с алертами")
    private void navigateToAlertsPage() {
        page.navigate("https://the-internet.herokuapp.com/javascript_alerts",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        assertEquals("JavaScript Alerts", page.locator("h3").textContent(),
                "Страница должна содержать заголовок 'JavaScript Alerts'");
        logExtent(Status.INFO, "Страница с алертами загружена");
    }

    @Step("Обработать JS Alert")
    private String foJsAlert() {
        CompletableFuture<String> alertMessageFuture = new CompletableFuture<>();

        // Тут устанавливаем обработчик диалога
        page.onDialog(dialog -> {
            String message = dialog.message();
            alertMessageFuture.complete(message);
            dialog.accept();
        });

        // Тут кликаем по кнопке, которая вызывает alert
        page.click("button[onclick='jsAlert()']");
        logExtent(Status.INFO, "Клик по кнопке JS Alert выполнен");

        // Тут ожидаем результат с таймаутом
        try {
            String alertMessage = alertMessageFuture.get(5, TimeUnit.SECONDS);
            logExtent(Status.INFO, "Получено сообщение алерта: " + alertMessage);
            return alertMessage;
        } catch (Exception e) {
            logExtent(Status.FAIL, "Таймаут ожидания алерта: " + e.getMessage());
            throw new RuntimeException("Alert не появился в течение 5 секунд", e);
        }
    }

    @Step("Проверить текст результата")
    private void verifyResultText() {
        page.waitForCondition(() ->
                page.locator("#result").textContent().contains("successfully"));

        String resultText = page.locator("#result").textContent();
        assertEquals("You successfully clicked an alert", resultText,
                "Текст результата должен соответствовать ожидаемому");
        logExtent(Status.INFO, "Результирующий текст проверен: " + resultText);
    }

    private void captureSuccessScreenshot() {
        String screenshotName = "success-screenshot.png";
        Path screenshotPath = Paths.get("allure-results", screenshotName);

        // Делаем скриншот
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotPath));

        logExtent(Status.INFO, "Скриншот успешного выполнения сохранен: " + screenshotPath);

        // Для Allure
        try (InputStream screenshotStream = new ByteArrayInputStream(screenshot)) {
            Allure.addAttachment("Успешное выполнение", "image/png", screenshotStream, ".png");
        } catch (Exception e) {
            logExtent(Status.WARNING, "Не удалось добавить скриншот в Allure: " + e.getMessage());
        }

        // Для ExtentReports
        String base64Screenshot = java.util.Base64.getEncoder().encodeToString(screenshot);
        test.pass("Скриншот успешного выполнения",
                MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
    }

    private void logExtent(Status status, String message) {
        test.log(status, message);
    }

    private void foTestFailure(Exception e) {
        // Скриншот для Allure при ошибке
        byte[] failureScreenshot = page.screenshot();

        try (InputStream failureStream = new ByteArrayInputStream(failureScreenshot)) {
            Allure.addAttachment("Ошибка теста", "image/png", failureStream, ".png");
        } catch (Exception ex) {
            logExtent(Status.WARNING, "Не удалось добавить скриншот ошибки в Allure: " + ex.getMessage());
        }

        // Логирование ошибки в ExtentReports
        String screenshotName = "error-screenshot.png";
        Path screenshotPath = Paths.get("allure-results", screenshotName);

        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath));

            String base64Screenshot = java.util.Base64.getEncoder().encodeToString(failureScreenshot);
            test.fail("Скриншот при ошибке",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
            test.fail(e);

        } catch (Exception ex) {
            logExtent(Status.WARNING, "Не удалось сохранить скриншот ошибки: " + ex.getMessage());
        }
    }

    @AfterEach
    void tearDownEach() {
        if (page != null) {
            page.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @AfterAll
    static void tearDown() {
        if (extent != null) {
            extent.flush();
        }
    }
}