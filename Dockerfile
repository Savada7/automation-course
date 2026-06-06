# Базовый образ с Playwright и браузерами
FROM mcr.microsoft.com/playwright/java:v1.50.0-jammy

# Установка зависимостей проекта
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean install