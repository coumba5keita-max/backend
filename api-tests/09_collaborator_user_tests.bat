@echo off
chcp 65001 >nul
title TESTS API - 09. Actions Collaborateur (ROLE_USER)
echo ==========================================================
echo  Lancement des tests : Actions Collaborateur (ROLE_USER)
echo ==========================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0\09_collaborator_user_tests.ps1"
echo.
pause
