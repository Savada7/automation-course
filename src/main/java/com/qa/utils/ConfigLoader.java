package com.qa.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    public static Properties load() {
        Properties props = new Properties();
        try (InputStream input = ConfigLoader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            props.load(input);  // Загружаем свойства из файла
        } catch (Exception e) {
            throw new RuntimeException("Config file not found!", e);
        }
        return props;
    }
}