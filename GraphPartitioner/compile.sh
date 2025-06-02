#!/bin/bash

# Skrypt kompilacji dla Graph Partitioner
# Użycie: ./compile.sh

echo "========================================="
echo "Kompilacja Graph Partitioner"
echo "========================================="

# Utwórz katalog bin jeśli nie istnieje
if [ ! -d "bin" ]; then
    echo "Tworzenie katalogu bin..."
    mkdir bin
fi

# Wyczyść stare pliki .class
echo "Czyszczenie starych plików..."
rm -rf bin/*

# Lista plików do kompilacji
echo "Szukanie plików Java..."
find src -name "*.java" > sources.txt

# Kompilacja
echo "Kompilowanie..."
javac -d bin -cp src @sources.txt

# Sprawdź czy kompilacja się powiodła
if [ $? -eq 0 ]; then
    echo "========================================="
    echo "Kompilacja zakończona pomyślnie!"
    echo "========================================="
    
    # Usuń plik tymczasowy
    rm sources.txt
    
    echo ""
    echo "Aby uruchomić aplikację, wykonaj:"
    echo "./run.sh"
else
    echo "========================================="
    echo "BŁĄD: Kompilacja nie powiodła się!"
    echo "========================================="
    
    # Usuń plik tymczasowy
    rm sources.txt
    
    exit 1
fi