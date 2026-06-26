@echo off
echo ========================================
echo Google Antigravity SDK - Gradle Upgrader
echo ========================================
echo.
echo Upgrading Gradle Wrapper to 8.10.2...
call gradlew wrapper --gradle-version 8.10.2 --distribution-type bin
echo.
echo Current Environment:
call gradlew --version
echo.