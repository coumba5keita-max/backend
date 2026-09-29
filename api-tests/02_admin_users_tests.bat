@echo off
chcp 65001 >nul
title TESTS API - 02. Gestion des Comptes (ROLE_ADMIN)
echo ==========================================================
echo  Lancement des tests : Gestion des Comptes (ROLE_ADMIN)
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\02_admin_users_tests.ps1"
echo.
pause
