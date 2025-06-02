@echo off
REM Skrypt uruchamiania dla Graph Partitioner (Windows)
REM Użycie: run.bat

echo =========================================
echo Uruchamianie Graph Partitioner
echo =========================================

REM Sprawdź czy katalog bin istnieje
if not exist "bin" (
    echo BLAD: Katalog bin nie istnieje!
    echo Najpierw skompiluj aplikacje uzywajac: compile.bat
    exit /b 1
)

REM Sprawdź czy główna klasa istnieje
if not exist "bin\com\example\graphpartitioner\ui\MainApplication.class" (
    echo BLAD: Aplikacja nie jest skompilowana!
    echo Najpierw skompiluj aplikacje uzywajac: compile.bat
    exit /b 1
)

REM Uruchom aplikację
echo Uruchamianie aplikacji...
java -cp bin com.example.graphpartitioner.ui.MainApplication

REM Sprawdź kod wyjścia
if %ERRORLEVEL% neq 0 (
    echo =========================================
    echo BLAD: Aplikacja zakonczyla sie bledem!
    echo =========================================
    exit /b 1
)