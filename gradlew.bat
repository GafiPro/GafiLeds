@echo off
setlocal enabledelayedexpansion
set "GRADLE_VERSION=9.6.1"
set "CACHE_DIR=%USERPROFILE%\.gradle\gafi-wrapper\%GRADLE_VERSION%"
set "DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%"
set "ZIP_PATH=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
if exist "%DIST_DIR%\bin\gradle.bat" goto run
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
if not exist "%ZIP_PATH%" (
  echo GafiLeds: Gradle %GRADLE_VERSION% nao encontrado localmente. A descarregar...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%URL%' -OutFile '%ZIP_PATH%'"
  if errorlevel 1 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_PATH%' -DestinationPath '%CACHE_DIR%' -Force"
if errorlevel 1 exit /b 1
:run
call "%DIST_DIR%\bin\gradle.bat" %*
endlocal
