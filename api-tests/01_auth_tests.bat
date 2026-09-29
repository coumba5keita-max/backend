@echo off
chcp 65001 >nul
title TESTS API - 01. Authentification
echo ==========================================================
echo  Lancement des tests API : Authentification & Inscription
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\01_auth_tests.ps1"
echo.
pause
