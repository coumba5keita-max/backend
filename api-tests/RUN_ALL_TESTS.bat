@echo off
chcp 65001 >nul
title SUITE DE TESTS API COMPLETE - TaskManager
echo ==============================================================================
echo       Lancement de la suite de tests complete (Toutes les API Backend)
echo ==============================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\RUN_ALL_TESTS.ps1"
echo.
pause
