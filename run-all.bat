@echo off
title MiniMart Launcher
echo ==============================================
echo   KHOI DONG HE THONG QUAN LY MINIMART
echo ==============================================
echo.

echo [1/2] Dang khoi dong Server trong cua so moi...
start "MiniMart Server" java "-Ddb.password=123456" -jar minimart-server\target\minimart-server.jar

echo [2/2] Cho Server khoi dong trong 3 giay...
timeout /t 3 /nobreak > nul

echo.
echo [OK] Dang khoi dong Client UI...
java -jar minimart-client\target\minimart-client.jar
