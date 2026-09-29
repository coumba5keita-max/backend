@echo off
chcp 65001 >nul
title TESTS API - 05. Gestion des Taches
echo ==========================================================
echo  Lancement des tests : Gestion des Taches
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\05_tasks_tests.ps1"
echo.
pause
