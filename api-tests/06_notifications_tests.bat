@echo off
chcp 65001 >nul
title TESTS API - 06. Notifications
echo ==========================================================
echo  Lancement des tests : Notifications
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\06_notifications_tests.ps1"
echo.
pause
