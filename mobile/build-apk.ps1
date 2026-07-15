param(
    [switch]$SkipDependencies
)

$ErrorActionPreference = "Stop"
$RootDir = $PSScriptRoot
$AndroidDir = Join-Path $RootDir "android"
$OutputDir = Join-Path $RootDir "dist"
$OutputApk = Join-Path $OutputDir "radiation-monitor.apk"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Assert-LastCommand([string]$Message) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Message (код $LASTEXITCODE)"
    }
}

function Find-AndroidSdk {
    $Candidates = @(
        @(
            $env:ANDROID_HOME,
            $env:ANDROID_SDK_ROOT,
            (Join-Path $env:LOCALAPPDATA "Android\Sdk")
        ) | Where-Object { $_ -and (Test-Path $_) }
    )

    if ($Candidates.Count -eq 0) {
        throw "Android SDK не найден. Установите Android Studio и Android SDK."
    }

    return (Resolve-Path $Candidates[0]).Path
}

function Find-SdkManager([string]$SdkDir) {
    $Candidates = @(
        (Join-Path $SdkDir "cmdline-tools\latest\bin\sdkmanager.bat"),
        (Join-Path $SdkDir "tools\bin\sdkmanager.bat")
    )

    $Versioned = Get-ChildItem (Join-Path $SdkDir "cmdline-tools") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "bin\sdkmanager.bat" }

    foreach ($Candidate in @($Candidates + $Versioned)) {
        if ($Candidate -and (Test-Path $Candidate)) {
            return $Candidate
        }
    }

    return $null
}

Set-Location $RootDir

Write-Step "Проверка Node.js, npm и Java"
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js не найден. Установите актуальную LTS-версию Node.js."
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "npm не найден. Переустановите Node.js с npm."
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $AndroidStudioJava = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path (Join-Path $AndroidStudioJava "bin\java.exe")) {
        $env:JAVA_HOME = $AndroidStudioJava
        $env:Path = "$AndroidStudioJava\bin;$env:Path"
    } else {
        throw "Java не найдена. Установите Java 17 или Android Studio."
    }
}

node --version
npm --version
java -version

Write-Step "Поиск Android SDK"
$SdkDir = Find-AndroidSdk
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir
$env:Path = "$(Join-Path $SdkDir 'platform-tools');$env:Path"
Write-Host "Android SDK: $SdkDir"

$SdkManager = Find-SdkManager $SdkDir
if ($SdkManager) {
    Write-Step "Принятие лицензий Android SDK"
    1..100 | ForEach-Object { "y" } | & $SdkManager "--sdk_root=$SdkDir" --licenses | Out-Host
    Assert-LastCommand "Не удалось принять лицензии Android SDK"

    Write-Step "Проверка Android SDK Platform 36, Build Tools и NDK"
    & $SdkManager "--sdk_root=$SdkDir" `
        "platform-tools" `
        "platforms;android-36" `
        "build-tools;36.0.0" `
        "ndk;27.1.12297006"
    Assert-LastCommand "Не удалось установить Android SDK components"
} else {
    Write-Warning "sdkmanager.bat не найден. Используются уже установленные Android-компоненты."
}

if (-not $SkipDependencies) {
    Write-Step "Установка npm-зависимостей"
    npm ci
    Assert-LastCommand "npm ci завершился с ошибкой"
}

if (-not (Test-Path (Join-Path $AndroidDir "gradlew.bat"))) {
    throw "Не найден android\gradlew.bat. Native Android-проект отсутствует."
}

Write-Step "Создание android/local.properties"
$SdkForProperties = $SdkDir.Replace("\", "/")
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    (Join-Path $AndroidDir "local.properties"),
    "sdk.dir=$SdkForProperties`n",
    $Utf8NoBom
)

Write-Step "Сборка release APK"
Push-Location $AndroidDir
try {
    & .\gradlew.bat assembleRelease
    Assert-LastCommand "Gradle-сборка завершилась с ошибкой"
} finally {
    Pop-Location
}

$GradleApk = Join-Path $AndroidDir "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $GradleApk)) {
    throw "Gradle завершился успешно, но APK не найден: $GradleApk"
}

Write-Step "Копирование APK"
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
Copy-Item -Force $GradleApk $OutputApk
$SizeMb = [Math]::Round((Get-Item $OutputApk).Length / 1MB, 1)
$Hash = (Get-FileHash $OutputApk -Algorithm SHA256).Hash

Write-Host "`nСборка завершена." -ForegroundColor Green
Write-Host "APK: $OutputApk"
Write-Host "Размер: $SizeMb MB"
Write-Host "SHA-256: $Hash"
