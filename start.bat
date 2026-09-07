@echo off
chcp 65001 >nul
title AI 智能饮食管理系统

rem ============================================================
rem  AI 智能饮食管理系统 - 一键启动脚本 (Windows)
rem  双击本脚本即可启动服务，并自动打开浏览器访问首页。
rem ============================================================

setlocal

rem 统一以脚本所在目录为工作目录(后端 SQLite 相对路径 data/diet.db 依赖于此)
cd /d "%~dp0"

rem 确保 SQLite 数据目录存在
if not exist "data" mkdir "data"

rem 定位 jar 包: 优先同目录，其次 backend/target 构建产物
set "JAR=%~dp0diet-backend-1.0.0.jar"
if not exist "%JAR%" set "JAR=%~dp0backend\target\diet-backend-1.0.0.jar"

if not exist "%JAR%" (
    echo [错误] 未找到可运行 jar 文件 diet-backend-1.0.0.jar。
    echo 请先构建项目：前端 npm run build + 后端 mvn package，
    echo 或将打包好的 jar 放到本脚本同目录后重试。
    pause
    exit /b 1
)

echo 正在检测 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Java，请先安装 Java 17（JDK 或 JRE）：
    echo        https://adoptium.net/
    pause
    exit /b 1
)

if "%AI_API_KEY%"=="" (
    echo [提示] 未配置 AI_API_KEY 环境变量，AI 相关功能（生成食谱/识菜/饮食建议）将不可用，
    echo        其余功能不受影响。如需启用 AI，请设置 AI_API_KEY 后重新运行本脚本。
)

echo.
echo 正在启动 AI 智能饮食管理系统...
echo 服务就绪后浏览器将自动打开 http://localhost:8080
echo 关闭本窗口或按 Ctrl+C 可停止服务。
echo.

rem 8 秒后自动打开浏览器（留出后端启动与数据库初始化时间）
start "" cmd /c "timeout /t 8 /nobreak >nul & start http://localhost:8080"

rem 前台运行 jar，Ctrl+C 或关闭窗口即停止
java -jar "%JAR%"

pause