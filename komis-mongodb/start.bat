@echo off
echo ================================================
echo 🚗 KOMPIS SAMOCHODOWY - MongoDB 8.2
echo ================================================
echo.

echo [1/4] Sprawdzanie MongoDB 8.2...
tasklist | findstr mongod.exe >nul
if errorlevel 1 (
    echo ❌ MongoDB nie jest uruchomione.
    echo ⚡ Uruchamiam MongoDB 8.2...
    
    start "MongoDB 8.2 Server" /B cmd /c "title MongoDB 8.2 && echo [MONGODB] Uruchamianie serwera wersja 8.2... && cd /d "C:\Program Files\MongoDB\Server\8.2\bin" && mongod.exe"
    
    echo ⏳ Czekam 5 sekund na uruchomienie...
    timeout /t 5 /nobreak >nul
    echo ✅ MongoDB 8.2 uruchomione
) else (
    echo ✅ MongoDB 8.2 już działa
)

echo.
echo [2/4] Sprawdzanie portu 3001...
netstat -an | findstr ":3001" >nul
if not errorlevel 1 (
    echo ⚠️  Port 3001 zajęty, używam 3002...
    set PORT=3002
) else (
    set PORT=3001
)

echo.
echo [3/4] Instalowanie zależności...
call npm install

echo.
echo [4/4] Uruchamianie serwera Node.js...
echo.
echo 🌐 Otwórz przeglądarkę: http://localhost:%PORT%
echo 📌 MongoDB działa na: localhost:27017
echo 🗄️  Baza danych: komis
echo 🛑 Aby zatrzymać: CTRL+C w tym oknie
echo.

node server.js

echo.
echo ================================================
echo 🛑 Serwer Node.js zatrzymany
echo ℹ️  MongoDB nadal działa w osobnym oknie
echo ================================================
pause