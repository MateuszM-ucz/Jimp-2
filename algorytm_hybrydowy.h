#ifndef ALGORYTM_HYBRYDOWY_H
#define ALGORYTM_HYBRYDOWY_H

#include "graf.h"
#include "podzial.h"

/* funkcja do wprowadzania perturbacji do istniejacego podzialu */
Podzial* perturbuj_podzial(Podzial* zrodlo, Graf* graf, double wspolczynnik_perturbacji);

/* glowna funkcja algorytmu hybrydowego - znajduje najlepszy podzial grafu */
Podzial* znajdz_najlepszy_podzial(Graf* graf, int liczba_czesci, int margines_procentowy);

#endif // ALGORYTM_HYBRYDOWY_H