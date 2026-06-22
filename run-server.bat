@echo off
title MiniMart Server
echo [MiniMart] Dang khoi dong Server...
java "-Ddb.password=123456" -jar minimart-server\target\minimart-server.jar
pause
