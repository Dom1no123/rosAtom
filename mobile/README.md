# Radiation Monitor — автономный Android APK

Приложение содержит локальный симулятор станций, зон, статистики и аварийных событий. После установки ему не нужны backend, интернет или Google Maps API key.

## Требования для сборки

- Node.js и npm;
- Java 17;
- Android SDK в `$ANDROID_HOME`, `$ANDROID_SDK_ROOT` или `~/Android/Sdk`;
- Android SDK Platform 36, Build Tools 36 и NDK 27.1.12297006.

Недостающие Android-компоненты Gradle предложит скачать при первой сборке.

## Установка зависимостей и проверки

```bash
cd mobile
npm ci
npm run typecheck
npm test
```

## Сборка APK без Expo-аккаунта

### Windows PowerShell

Откройте PowerShell в папке `mobile` и выполните:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

Скрипт сам найдёт Android SDK, установит npm- и Android-зависимости, создаст `local.properties`, соберёт APK и выведет его SHA-256. Для повторной сборки без `npm ci`:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -SkipDependencies
```

### Linux

```bash
npm run build:apk
```

Скрипт находит Android SDK, создаёт локальный `android/local.properties`, запускает release-сборку и копирует результат сюда:

```text
mobile/dist/radiation-monitor.apk
```

Первая сборка может занять продолжительное время, потому что Gradle скачивает native-зависимости. Последующие сборки используют локальный кэш.

Прямая команда Gradle, если она понадобится:

```bash
cd mobile/android
ANDROID_HOME="$HOME/Android/Sdk" ./gradlew assembleRelease
```

Исходный Gradle-артефакт находится в `mobile/android/app/build/outputs/apk/release/app-release.apk`.

## Запуск на подключённом Android-устройстве

```bash
npm run android
```

## Демонстрация

1. Откройте приложение в airplane mode.
2. Проверьте главную, офлайн-карту, статистику и карточку станции.
3. На экране «Инструкции» запустите тестовое ЧП.
4. Подтвердите полноэкранную инструкцию и откройте красную зону на карте.
5. Через 60 секунд зона автоматически вернётся к безопасному уровню.
6. В настройках можно включить автосимуляцию с событиями через 2–5 минут.

Состояние симуляции сохраняется в AsyncStorage и восстанавливается после перезапуска.
