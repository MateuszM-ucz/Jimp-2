#!/bin/bash

# Skrypt uruchamiania dla Graph Partitioner
# Użycie: ./run.sh

echo "========================================="
echo "Uruchamianie Graph Partitioner"
echo "========================================="

# Sprawdź czy katalog bin istnieje
if [ ! -d "bin" ]; then
    echo "BŁĄD: Katalog bin nie istnieje!"
    echo "Najpierw skompiluj aplikację używając: ./compile.sh"
    exit 1
fi

# Sprawdź czy główna klasa istnieje
if [ ! -f "bin/com/example/graphpartitioner/ui/MainApplication.class" ]; then
    echo "BŁĄD: Aplikacja nie jest skompilowana!"
    echo "Najpierw skompiluj aplikację używając: ./compile.sh"
    exit 1
fi

# Uruchom aplikację
echo "Uruchamianie aplikacji..."
java -cp bin com.example.graphpartitioner.ui.MainApplication

# Sprawdź kod wyjścia
if [ $? -ne 0 ]; then
    echo "========================================="
    echo "BŁĄD: Aplikacja zakończyła się błędem!"
    echo "========================================="
    exit 1
fi