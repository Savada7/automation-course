package com.qa.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class FileUploadTest {
    static Playwright playwright;
    static APIRequestContext request;

    @BeforeAll
    static void setup() {
        playwright = Playwright.create();
        request = playwright.request().newContext();
    }

    @Test
    void testPngFileUploadAndDownload() {
        // Генерируем тестовый PNG-файл в памяти
        byte[] originalPng = createTestPng(80, 60);

        // Загружаем файл на сервер через multipart/form-data
        APIResponse uploadResponse = request.post(
                "https://httpbin.org/post",
                RequestOptions.create()
                        .setMultipart(
                                FormData.create()
                                        .set("file", new FilePayload(
                                                "test.png",
                                                "image/png",
                                                originalPng
                                        ))
                        )
        );

        // Проверяем получение файла сервером
        assertEquals(200, uploadResponse.status());

        String responseBody = uploadResponse.text();
        assertTrue(responseBody.contains("data:image/png;base64,"));

        // Проверяем точное соответствие содержимого
        String base64Data = extractBase64Data(responseBody);
        byte[] uploadedData = Base64.getDecoder().decode(base64Data);

        assertArrayEquals(originalPng, uploadedData,
                "Загруженный файл должен совпадать с исходным");

        // Скачиваем эталонный PNG-файл
        APIResponse downloadResponse = request.get("https://httpbin.org/image/png");

        // Проверяем MIME-тип
        String contentType = downloadResponse.headers().get("content-type");
        assertEquals("image/png", contentType);

        // Проверяем сигнатуру PNG
        byte[] downloadedData = downloadResponse.body();
        assertTrue(isValidPng(downloadedData),
                "Скачанный файл должен быть валидным PNG");
    }

    private byte[] createTestPng(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            g.setColor(Color.GREEN);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.RED);
            g.drawOval(10, 10, width-20, height-20);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Не удалось создать PNG", e);
        }
    }

    private String extractBase64Data(String responseBody) {
        int start = responseBody.indexOf("data:image/png;base64,") + "data:image/png;base64,".length();
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end);
    }

    private boolean isValidPng(byte[] data) {
        // Проверяем PNG сигнатуру
        if (data.length < 8) return false;

        byte[] pngSignature = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        for (int i = 0; i < 8; i++) {
            if (data[i] != pngSignature[i]) {
                return false;
            }
        }
        return true;
    }

    @AfterAll
    static void tearDownAll() {
        if (request != null) request.dispose();
        if (playwright != null) playwright.close();
    }
}