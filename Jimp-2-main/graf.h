#ifndef GRAF_H
#define GRAF_H

typedef struct {
    int liczba_wierzcholkow;
    int liczba_krawedzi;
    int* wskazniki_listy;
    int* lista_sasiedztwa;
} Graf;

// Function declarations
Graf* wczytaj_graf(const char* nazwa_pliku);
void zwolnij_graf(Graf* graf);

#endif