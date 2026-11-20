@echo off
setlocal
cd /d "%~dp0"
set "mode=%~1"
if "%mode%"=="" set "mode=run"
if not exist out mkdir out
javac --release 17 -encoding UTF-8 -d out -sourcepath src src\taskflow\TaskFlow.java
if errorlevel 1 exit /b 1
if /i "%mode%"=="run" goto run
if /i "%mode%"=="test" goto tests
if /i "%mode%"=="ui-test" goto tests
if /i "%mode%"=="docs" goto docs
echo Usage: taskflow.cmd [run^|test^|ui-test [image-directory]^|docs]
exit /b 2
:run
java -cp out taskflow.TaskFlow
exit /b %errorlevel%
:tests
javac --release 17 -encoding UTF-8 -cp out -d out test\taskflow\TaskChecks.java test\taskflow\DesktopChecks.java
if errorlevel 1 exit /b 1
if /i "%mode%"=="test" (
    java -cp out taskflow.TaskChecks
) else if "%~2"=="" (
    java -cp out taskflow.DesktopChecks
) else (
    java -cp out taskflow.DesktopChecks "%~2"
)
exit /b %errorlevel%
:docs
javadoc -quiet -Werror -Xdoclint:all -notimestamp --no-platform-links -author -package -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d docs\api -sourcepath src taskflow
exit /b %errorlevel%
