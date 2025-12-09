package courseplayw;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHomePageVisual extends BaseTest{
    @Test
    void checkScreenshot() throws IOException {
        // 1. Открыть страницу и сделать скриншот
        page.navigate("https://the-internet.herokuapp.com/");
        byte[] currentScreenshot = page.screenshot();

        // 2. Путь к эталону
        Path referencePath = Paths.get("src/test/resources/reference.png");

        // 3. Если эталона нет - создать
        if (!Files.exists(referencePath)) {
            System.out.println("Создаю эталонный скриншот...");
            Files.createDirectories(referencePath.getParent());
            Files.write(referencePath, currentScreenshot);
            return; // Тест завершен
        }

        // 4. Прочитать эталон
        byte[] referenceScreenshot = Files.readAllBytes(referencePath);

        // 5. Простое сравнение массивов байтов
        if (currentScreenshot.length != referenceScreenshot.length) {
            throw new RuntimeException("Размеры скриншотов разные!");
        }

        // 6. Поиск первого различия
        for (int i = 0; i < currentScreenshot.length; i++) {
            if (currentScreenshot[i] != referenceScreenshot[i]) {
                throw new RuntimeException("Скриншоты разные! Первое отличие на байте " + i);
            }
        }

        System.out.println("✅ Все ок, скриншоты одинаковые!");
    }
}

