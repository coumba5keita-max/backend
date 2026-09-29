@echo off
chcp 65001 >nul
title TESTS API - 04. Projets & Membres (ROLE_ADMIN)
echo ==========================================================
echo  Lancement des tests : Projets & Membres (ROLE_ADMIN)
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\04_admin_projects_tests.ps1"
echo.
pause
