@echo off
REM Skrypt kompilacji dla Graph Partitioner (Windows)
REM Użycie: compile.bat

echo =========================================
echo Kompilacja Graph Partitioner
echo =========================================

REM Utwórz katalog bin jeśli nie istnieje
if not exist "bin" (
    echo Tworzenie katalogu bin...
    mkdir bin
)

REM Wyczyść stare pliki .class
echo Czyszczenie starych plikow...
if exist "bin\*" del /q /s bin\* >nul 2>&1

REM Znajdź wszystkie pliki Java
echo Szukanie plikow Java...
dir /s /b src\*.java > sources.txt

REM Kompilacja
echo Kompilowanie...
javac -d bin -cp src @sources.txt

REM Sprawdź czy kompilacja się powiodła
if %ERRORLEVEL% equ 0 (
    echo =========================================
    echo Kompilacja zakonczona pomyslnie!
    echo =========================================
    
    REM Usuń plik tymczasowy
    del sources.txt
    
    echo.
    echo Aby uruchomic aplikacje, wykonaj:
    echo run.bat
) else (
    echo =========================================
    echo BLAD: Kompilacja nie powiodla sie!
    echo =========================================
    
    REM Usuń plik tymczasowy
    del sources.txt
    
    exit /b 1
)