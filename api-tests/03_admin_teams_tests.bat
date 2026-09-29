@echo off
chcp 65001 >nul
title TESTS API - 03. Gestion des Equipes (ROLE_ADMIN)
echo ==========================================================
echo  Lancement des tests : Gestion des Equipes (ROLE_ADMIN)
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\03_admin_teams_tests.ps1"
echo.
pause
