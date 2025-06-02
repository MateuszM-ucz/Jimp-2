# Graph Partitioner - Wizualizacja i partycjonowanie grafów

Aplikacja Java Swing do wizualizacji i partycjonowania grafów przy użyciu algorytmu Kernighana-Lina oraz algorytmu hybrydowego.

## Funkcjonalności

- **Wczytywanie grafów** z 4 różnych formatów:
  - Tekstowy CSR z macierzą sąsiedztwa
  - CSRRG (Compressed Sparse Row Row Graph)
  - Proste przypisanie tekstowe
  - Proste przypisanie binarne

- **Algorytmy partycjonowania**:
  - Modulo
  - Sekwencyjny
  - Losowy
  - DFS
  - Hybrydowy (automatycznie wybiera najlepszą strategię)

- **Optymalizacja** przy użyciu algorytmu Kernighana-Lina

- **Interaktywna wizualizacja**:
  - Przeciąganie wierzchołków
  - Zoom (kółko myszy)
  - Pan (prawy przycisk myszy)
  - Kolorowanie części

- **Zapis wyników** w formatach:
  - Tekstowy (macierz + przypisania)
  - CSRRG

## Wymagania

- Java 8 lub nowsza
- System operacyjny: Windows, Linux lub macOS

## Kompilacja i uruchomienie

### Linux/macOS

1. Nadaj uprawnienia wykonywania skryptom:
```bash
chmod +x compile.sh run.sh
```

2. Skompiluj aplikację:
```bash
./compile.sh
```

3. Uruchom aplikację:
```bash
./run.sh
```

### Windows

1. Skompiluj aplikację:
```cmd
compile.bat
```

2. Uruchom aplikację:
```cmd
run.bat
```

### VS Code

Projekt zawiera konfigurację dla VS Code. Po otwarciu folderu w VS Code:
1. Zainstaluj rozszerzenie "Extension Pack for Java"
2. Użyj `Ctrl+Shift+B` do kompilacji
3. Użyj `F5` do uruchomienia

## Struktura projektu

```
GraphPartitioner/
├── src/                    # Kod źródłowy
│   └── com/example/graphpartitioner/
│       ├── model/         # Klasy modelu (Graph, Partition)
│       ├── io/            # Wczytywanie i zapis plików
│       ├── algorithms/    # Algorytmy partycjonowania
│       ├── ui/            # Interfejs użytkownika
│       └── utils/         # Klasy pomocnicze
├── bin/                   # Skompilowane pliki .class
├── compile.sh/bat         # Skrypty kompilacji
├── run.sh/bat            # Skrypty uruchamiania
└── README.md             # Ten plik
```

## Użytkowanie

1. **Wczytaj graf** z menu `Plik`:
   - Wybierz odpowiedni format pliku
   - Dla plików tekstowych CSR możesz wczytać graf z istniejącym podziałem

2. **Skonfiguruj parametry** w panelu narzędzi:
   - Liczba części (2-100)
   - Margines procentowy (0-100%)
   - Algorytm inicjalizacji
   - Opcja użycia algorytmu hybrydowego

3. **Wykonaj partycjonowanie**:
   - Kliknij przycisk "Partycjonuj graf"
   - Obserwuj postęp w panelu szczegółów

4. **Interakcja z wizualizacją**:
   - Przeciągaj wierzchołki lewym przyciskiem myszy
   - Zoom kółkiem myszy
   - Pan prawym przyciskiem myszy
   - Menu `Widok` > `Resetuj widok` lub `Dopasuj do okna`

5. **Zapisz wyniki**:
   - Menu `Plik` > `Zapisz podział jako...`
   - Wybierz format tekstowy lub CSRRG

## Formaty plików

### Tekstowy CSR (przykład)
```
[0. 1. 1. 0.]
[1. 0. 1. 1.]
[1. 1. 0. 1.]
[0. 1. 1. 0.]

0 - 0
1 - 0
2 - 1
3 - 1
```

### CSRRG
Format 5-liniowy dla grafu głównego + 2 linie na każdy podgraf.

### Proste przypisanie tekstowe
```
Wierzchołek 0 -> Podgraf 0
Wierzchołek 1 -> Podgraf 1
...
```

## Algorytmy

- **Kernighan-Lin**: Iteracyjna optymalizacja minimalizująca liczbę przeciętych krawędzi
- **Algorytm hybrydowy**: Testuje różne strategie inicjalizacji i perturbacje, wybiera najlepszy wynik

## Wskazówki

- Dla małych grafów (<100 wierzchołków) wszystkie algorytmy działają szybko
- Dla dużych grafów (>1000 wierzchołków) algorytm hybrydowy może być wolniejszy
- Margines procentowy kontroluje dozwoloną różnicę wielkości między częściami
- Przecięte krawędzie są zaznaczone czerwoną przerywaną linią

## Rozwiązywanie problemów

1. **"Błąd: Aplikacja nie jest skompilowana!"**
   - Uruchom skrypt kompilacji przed uruchomieniem

2. **OutOfMemoryError dla dużych grafów**
   - Uruchom z większą pamięcią: `java -Xmx2g -cp bin com.example.graphpartitioner.ui.MainApplication`

3. **Brak reakcji przy dużych grafach**
   - Partycjonowanie dużych grafów może trwać kilka sekund
   - Obserwuj panel szczegółów dla informacji o postępie