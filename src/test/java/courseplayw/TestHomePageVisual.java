package courseplayw;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestHomePageVisual extends BaseTest {

    @Test
    void checkScreenshot() throws IOException {
        page.navigate("https://the-internet.herokuapp.com/");
        byte[] screenshot = page.screenshot();

        Path current = Paths.get("target/current.png");
        Files.write(current, screenshot);

        Path reference = Paths.get("src/test/resources/reference.png");

        if (!Files.exists(reference)) {
            Files.createDirectories(reference.getParent());
            Files.copy(current, reference);
            System.out.println("Эталон создан");
            return;
        }

        // Используем Files.mismatch() для сравнения
        long result = Files.mismatch(current, reference);

        if (result != -1) {
            // Создаем простой diff файл
            String diff = String.format(
                    "Файлы разные!\n" +
                            "Отличие на байте: %d\n" +
                            "Текущий размер: %d байт\n" +
                            "Эталонный размер: %d байт\n",
                    result,
                    Files.size(current),
                    Files.size(reference)
            );

            Files.writeString(Paths.get("target/diff.txt"), diff);
            throw new RuntimeException("Скриншоты различаются!\n" + diff);
        }

        System.out.println("OK");
    }
}