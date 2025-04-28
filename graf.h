#ifndef GRAF_H
#define GRAF_H

// Struktura reprezentujaca graf w formacie CSR (Compressed Sparse Row)
typedef struct {
    int liczba_wierzcholkow;    // Liczba wierzcholkow grafu
    int liczba_krawedzi;        // Liczba krawedzi grafu 
    int* lista_sasiedztwa;      // Lista sasiedztwa (wartosci)
    int* wskazniki_listy;       // Wskazniki na poczatek listy sasiedztwa dla kazdego wierzcholka
} Graf;

// Funkcje podstawowe dla grafu
Graf* wczytaj_graf(const char* nazwa_pliku);
void zwolnij_graf(Graf* graf);

#endif // GRAF_H