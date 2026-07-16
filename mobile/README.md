# Radiation Monitor — нативный автономный Android APK

Мобильное приложение написано на Kotlin и Jetpack Compose. Expo, React Native, Node.js, EAS, backend и Google Maps API не нужны. Данные и аварийные сценарии работают автономно; интернет используется только для первой загрузки подложки OpenStreetMap.

## Требования

- Java 17 (подойдёт JBR из Android Studio);
- Android SDK Platform 36 и Build Tools 36;
- Android SDK в `ANDROID_HOME`, `ANDROID_SDK_ROOT` или стандартной папке Android Studio.

NDK, CMake и Node.js не требуются.

## Windows — одна команда PowerShell

Откройте PowerShell в папке `mobile`:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

Скрипт найдёт Java и Android SDK, установит недостающие Platform/Build Tools, создаст `local.properties`, соберёт APK и выведет SHA-256.

## Linux

Из папки `mobile`:

```bash
bash scripts/build-apk.sh
```

Прямая Gradle-команда:

```bash
cd android
./gradlew assembleDebug
```

Готовый APK в обоих случаях:

```text
mobile/dist/radiation-monitor.apk
```

Исходный Gradle-артефакт:

```text
mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

## Установка на телефон

Подключите телефон с включённой USB-отладкой:

```bash
adb install -r dist/radiation-monitor.apk
```

## Что работает автономно

- 20 станций и 5 радиационных зон;
- локальные измерения, история и недельная статистика;
- карта OpenStreetMap с локальным кэшем просмотренных тайлов минимум на 7 дней;
- журнал аварий, ручная и автоматическая симуляция;
- полноэкранная инструкция, локальная сирена и Android-уведомления;
- светлая/тёмная тема и настройки в `SharedPreferences`.

На экране «Инструкции» кнопка тестового ЧП позволяет проверить весь аварийный сценарий без сервера.
