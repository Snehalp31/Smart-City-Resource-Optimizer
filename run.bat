@echo off
setlocal
where javac >nul 2>nul
if errorlevel 1 (
  echo JDK 17 or later is required. Install a JDK and reopen this terminal.
  exit /b 1
)
if exist pom.xml (
  where mvn >nul 2>nul
  if not errorlevel 1 (
    call mvn -q compile
    if errorlevel 1 exit /b 1
    call mvn -q exec:java
    exit /b %errorlevel%
  )
)
javac Main.java MainGUIPastel.java MapServer.java JdbcStore.java WeatherService.java
if errorlevel 1 exit /b 1
java MainGUIPastel
