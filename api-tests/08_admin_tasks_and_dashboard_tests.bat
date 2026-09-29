@echo off
chcp 65001 >nul
title TESTS API - 08. Taches, Dashboard et Reporting (ROLE_ADMIN)
echo ==========================================================
echo  Lancement des tests : Taches, Dashboard et Reporting
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\08_admin_tasks_and_dashboard_tests.ps1"
echo.
pause
