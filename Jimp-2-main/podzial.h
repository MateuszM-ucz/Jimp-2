#ifndef PODZIAL_H
#define PODZIAL_H

#include "graf.h"

// Struktura reprezentujaca podzial grafu
typedef struct {
    int* przypisania;          // Do ktorej czesci nalezy kazdy wierzcholek
    int liczba_czesci;         // Liczba czesci
    int* rozmiary_czesci;      // Liczba wierzcholkow w kazdej czesci
    int przeciete_krawedzie;   // Liczba przecietych krawedzi
    int margines_procentowy;   // Maksymalny dozwolony margines procentowy
} Podzial;

// Funkcje inicjalizacji podzialu
Podzial* inicjalizuj_podzial_modulo(Graf* graf, int liczba_czesci, int margines_procentowy);
Podzial* inicjalizuj_podzial_sekwencyjny(Graf* graf, int liczba_czesci, int margines_procentowy);
Podzial* inicjalizuj_podzial_losowy(Graf* graf, int liczba_czesci, int margines_procentowy);
Podzial* kopiuj_podzial(Podzial* zrodlo, int liczba_wierzcholkow);

// Funkcje pomocnicze
int oblicz_przeciete_krawedzie(Graf* graf, Podzial* podzial);
void wyswietl_podzial(Graf* graf, Podzial* podzial);
void zapisz_podzial(const char* nazwa_pliku, Graf* graf, Podzial* podzial, const char* format);
void zwolnij_podzial(Podzial* podzial);
void zapisz_podzial_tekstowy(const char* nazwa_pliku, Graf* graf, Podzial* podzial);
void zapisz_podzial_binarny(const char* nazwa_pliku, Graf* graf, Podzial* podzial);
#endif // PODZIAL_H