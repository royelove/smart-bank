@echo off
chcp 65001 >nul
cd /d %~dp0
echo 启动人脸识别服务 http://localhost:8001 ...
.venv\Scripts\python.exe app.py
pause
