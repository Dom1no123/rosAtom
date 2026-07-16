#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}}"

if [[ ! -d "$SDK_DIR" ]]; then
  echo "Android SDK не найден: $SDK_DIR" >&2
  echo "Установите Android SDK или задайте ANDROID_HOME." >&2
  exit 1
fi

printf 'sdk.dir=%s\n' "$SDK_DIR" > "$ROOT_DIR/android/local.properties"

cd "$ROOT_DIR/android"
./gradlew assembleDebug

mkdir -p "$ROOT_DIR/dist"
cp app/build/outputs/apk/debug/app-debug.apk "$ROOT_DIR/dist/radiation-monitor.apk"

echo
echo "APK готов: $ROOT_DIR/dist/radiation-monitor.apk"
