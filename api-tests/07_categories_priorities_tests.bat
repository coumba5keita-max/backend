@echo off
chcp 65001 >nul
title TESTS API - 07. Categories et Priorites (ROLE_ADMIN)
echo ==========================================================
echo  Lancement des tests : Categories et Priorites
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\07_categories_priorities_tests.ps1"
echo.
pause
