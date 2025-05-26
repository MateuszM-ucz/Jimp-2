#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <time.h>
#include <string.h>
#include "algorytm_hybrydowy.h"
#include "algorytm_kl.h"

/* funkcja wprowadzajaca perturbacje do istniejacego podzialu */
Podzial* perturbuj_podzial(Podzial* zrodlo, Graf* graf, double wspolczynnik_perturbacji){
    if(!zrodlo || !graf || graf->liczba_wierzcholkow == 0 || zrodlo->liczba_czesci <= 1){
        fprintf(stderr, "Nieprawidlowe parametry w perturbuj_podzial\n");
        return NULL;
    }

    Podzial* perturbowany = kopiuj_podzial(zrodlo, graf->liczba_wierzcholkow);
    if(!perturbowany) return NULL;

    /* oblicz liczbe wierzcholkow do perturbacji */
    int wierzcholki_do_perturbacji = (int)(graf->liczba_wierzcholkow * wspolczynnik_perturbacji);
    if(wierzcholki_do_perturbacji < 1 && wspolczynnik_perturbacji > 0 && graf->liczba_wierzcholkow > 0){
        wierzcholki_do_perturbacji = 1;
    }
    if(wierzcholki_do_perturbacji > graf->liczba_wierzcholkow){
        wierzcholki_do_perturbacji = graf->liczba_wierzcholkow;
    }

    /* oblicz sredni rozmiar czesci dla ograniczen rownowagi */
    int sredni_rozmiar = graf->liczba_wierzcholkow / perturbowany->liczba_czesci;
    int max_nierownosc = (sredni_rozmiar * perturbowany->margines_procentowy) / 100;
    if(max_nierownosc < 1) max_nierownosc = 1;

    printf("Perturbacja: wykonywanie %d losowych ruchow...\n", wierzcholki_do_perturbacji);
    
    int ruchy_wykonane = 0;
    int max_prob = wierzcholki_do_perturbacji * 10; /* limit prob, aby uniknac nieskonczonej petli */
    int proby = 0;
    
    while(ruchy_wykonane < wierzcholki_do_perturbacji && proby < max_prob){
        proby++;
        
        /* wybierz losowy wierzcholek */
        int v = rand() % graf->liczba_wierzcholkow;
        
        int src_part = perturbowany->przypisania[v];
        
        /* znajdz losowa czesc docelowa rozna od obecnej */
        int dest_part = src_part;
        while(dest_part == src_part && perturbowany->liczba_czesci > 1){
            dest_part = rand() % perturbowany->liczba_czesci;
        }
        
        /* sprawdz czy przeniesienie tego wierzcholka nie narusza ograniczen rownowagi */
        if(perturbowany->rozmiary_czesci[dest_part] >= sredni_rozmiar + max_nierownosc ||
            perturbowany->rozmiary_czesci[src_part] <= sredni_rozmiar - max_nierownosc){
            continue; /* nie mozemy przeniesc tego wierzcholka */
        }
        
        /* przenies wierzcholek */
        perturbowany->przypisania[v] = dest_part;
        perturbowany->rozmiary_czesci[src_part]--;
        perturbowany->rozmiary_czesci[dest_part]++;
        ruchy_wykonane++;
    }
    
    printf("Perturbacja: wykonano %d/%d ruchow po %d probach\n", 
           ruchy_wykonane, wierzcholki_do_perturbacji, proby);
    
    /* aktualizuj liczbe przecietych krawedzi */
    perturbowany->przeciete_krawedzie = oblicz_przeciete_krawedzie(graf, perturbowany);
    
    return perturbowany;
}

/* funkcja adaptacyjnie okreslajaca liczbe prob dla metody losowej */
int oblicz_adaptacyjna_liczbe_prob(Graf* graf, int liczba_czesci, int najlepsze_dotychczas){
    /* oblicz gestosc grafu */
    double gestosc = 0.0;
    if(graf->liczba_wierzcholkow > 1){
        double max_krawedzi = (double)graf->liczba_wierzcholkow * (graf->liczba_wierzcholkow - 1) / 2.0;
        gestosc = (double)graf->liczba_krawedzi / max_krawedzi;
    }
    
    /* dla rzadszych grafow, probuj wiecej losowych inicjalizacji */
    int podstawowe_proby = 3; /* domyslnie */
    
    if(gestosc < 0.01){
        podstawowe_proby = 5;
    }else if(gestosc < 0.1){
        podstawowe_proby = 4;
    }
    
    /* dla wiekszej liczby czesci, probuj wiecej inicjalizacji */
    if(liczba_czesci > 10){
        podstawowe_proby += 2;
    }else if(liczba_czesci > 5){
        podstawowe_proby += 1;
    }
    
    /* jesli nie znalezlismy dobrego rozwiazania, probuj wiecej */
    if(najlepsze_dotychczas == INT_MAX || najlepsze_dotychczas > graf->liczba_krawedzi / 2){
        podstawowe_proby += 1;
    }
    
    printf("Obliczono liczbe prob adaptacyjnie: %d (gestosc grafu: %.4f, liczba czesci: %d)\n", 
           podstawowe_proby, gestosc, liczba_czesci);
    
    return podstawowe_proby;
}

/* glowna funkcja znajdujaca najlepszy podzial za pomoca metod hybrydowych */
Podzial* znajdz_najlepszy_podzial(Graf* graf, int liczba_czesci, int margines_procentowy){
    if(!graf || graf->liczba_wierzcholkow <= 0 || liczba_czesci <= 0 || margines_procentowy < 0){
        fprintf(stderr, "Nieprawidlowe parametry w znajdz_najlepszy_podzial\n");
        return NULL;
    }
    
    Podzial* najlepszy_podzial = NULL;
    int najlepsze_przeciete_krawedzie = INT_MAX;
    
    printf("Szukanie optymalnego podzialu grafu metoda hybrydowa...\n");
    
    /* 1. najpierw wyprobuj deterministyczne strategie */
    printf("Krok 1: Wyprobowywanie deterministycznych strategii...\n");
    
    /* 1.1 strategia modulo */
    printf("  - Strategia modulo...\n");
    Podzial* podzial_modulo = inicjalizuj_podzial_modulo(graf, liczba_czesci, margines_procentowy);
    if(podzial_modulo){
        printf("    Poczatkowa liczba przecietych krawedzi: %d\n", podzial_modulo->przeciete_krawedzie);
        algorytm_kernighan_lin(graf, podzial_modulo);
        printf("    Koncowa liczba przecietych krawedzi: %d\n", podzial_modulo->przeciete_krawedzie);
        
        if(podzial_modulo->przeciete_krawedzie < najlepsze_przeciete_krawedzie){
            najlepsze_przeciete_krawedzie = podzial_modulo->przeciete_krawedzie;
            najlepszy_podzial = podzial_modulo;
        }else{
            zwolnij_podzial(podzial_modulo);
        }
    }
    
    /* 1.2 strategia sekwencyjna */
    printf("  - Strategia sekwencyjna...\n");
    Podzial* podzial_sekwencyjny = inicjalizuj_podzial_sekwencyjny(graf, liczba_czesci, margines_procentowy);
    if(podzial_sekwencyjny){
        printf("    Poczatkowa liczba przecietych krawedzi: %d\n", podzial_sekwencyjny->przeciete_krawedzie);
        algorytm_kernighan_lin(graf, podzial_sekwencyjny);
        printf("    Koncowa liczba przecietych krawedzi: %d\n", podzial_sekwencyjny->przeciete_krawedzie);
        
        if(podzial_sekwencyjny->przeciete_krawedzie < najlepsze_przeciete_krawedzie){
            if(najlepszy_podzial) zwolnij_podzial(najlepszy_podzial);
            najlepsze_przeciete_krawedzie = podzial_sekwencyjny->przeciete_krawedzie;
            najlepszy_podzial = podzial_sekwencyjny;
        }else{
            zwolnij_podzial(podzial_sekwencyjny);
        }
    }
    
    /* 2. nastepnie wyprobuj losowe inicjalizacje z adaptacyjna liczba prob */
    printf("Krok 2: Wyprobowywanie losowych inicjalizacji...\n");
    
    /* oblicz adaptacyjnie liczbe losowych inicjalizacji do wyprobowania */
    int liczba_prob_losowych = oblicz_adaptacyjna_liczbe_prob(graf, liczba_czesci, najlepsze_przeciete_krawedzie);
    
    /* dla duzych grafow ograniczamy liczbe prob */
    if(graf->liczba_wierzcholkow > 10000){
        liczba_prob_losowych = (liczba_prob_losowych > 2) ? 2 : liczba_prob_losowych;
        printf("Duzy graf (%d wierzcholkow) - ograniczenie do %d prob losowych\n", 
               graf->liczba_wierzcholkow, liczba_prob_losowych);
    }
    
    for(int seed = 0; seed < liczba_prob_losowych; seed++){
        printf("  - Losowa inicjalizacja %d/%d...\n", seed + 1, liczba_prob_losowych);
        Podzial* podzial_losowy = inicjalizuj_podzial_losowy(graf, liczba_czesci, margines_procentowy);
        if(podzial_losowy){
            printf("    Poczatkowa liczba przecietych krawedzi: %d\n", podzial_losowy->przeciete_krawedzie);
            algorytm_kernighan_lin(graf, podzial_losowy);
            printf("    Koncowa liczba przecietych krawedzi: %d\n", podzial_losowy->przeciete_krawedzie);
            
            if(podzial_losowy->przeciete_krawedzie < najlepsze_przeciete_krawedzie){
                if(najlepszy_podzial) zwolnij_podzial(najlepszy_podzial);
                najlepsze_przeciete_krawedzie = podzial_losowy->przeciete_krawedzie;
                najlepszy_podzial = podzial_losowy;
            }else{
                zwolnij_podzial(podzial_losowy);
            }
        }
    }
    
    /* 3. na koniec, sprobuj perturbacji najlepszego znalezionego rozwiazania */
    if(najlepszy_podzial){
        printf("Krok 3: Testowanie perturbacji najlepszego rozwiazania...\n");
        
        /* okresL liczbe perturbacji do wyprobowania */
        int liczba_perturbacji = 2;
        if(graf->liczba_wierzcholkow > 1000) liczba_perturbacji = 3;
        
        for(int i = 0; i < liczba_perturbacji; i++){
            printf("  - Perturbacja %d/%d...\n", i + 1, liczba_perturbacji);
            
            /* uzyj wyzszego wspolczynnika perturbacji dla pierwszej proby */
            double wspolczynnik = (i == 0) ? 0.15 : 0.1;
            
            Podzial* perturbowany = perturbuj_podzial(najlepszy_podzial, graf, wspolczynnik);
            if(perturbowany){
                printf("    Poczatkowa liczba przecietych krawedzi po perturbacji: %d\n", perturbowany->przeciete_krawedzie);
                algorytm_kernighan_lin(graf, perturbowany);
                printf("    Koncowa liczba przecietych krawedzi: %d\n", perturbowany->przeciete_krawedzie);
                
                if(perturbowany->przeciete_krawedzie < najlepsze_przeciete_krawedzie){
                    zwolnij_podzial(najlepszy_podzial);
                    najlepsze_przeciete_krawedzie = perturbowany->przeciete_krawedzie;
                    najlepszy_podzial = perturbowany;
                    printf("    Znaleziono nowe najlepsze rozwiazanie!\n");
                }else{
                    zwolnij_podzial(perturbowany);
                }
            }
        }
    }
    
    if(najlepszy_podzial){
        printf("Najlepszy znaleziony podzial ma %d przecietych krawedzi.\n", najlepsze_przeciete_krawedzie);
    }else{
        fprintf(stderr, "Nie udalo sie znalezc zadnego podzialu!\n");
    }
    
    /* weryfikacja poprawnosci koncowego podzialu */
    if(najlepszy_podzial){
        /* dodatkowa weryfikacja liczby przecietych krawedzi */
        int weryfikacja = oblicz_przeciete_krawedzie(graf, najlepszy_podzial);
        if(weryfikacja != najlepszy_podzial->przeciete_krawedzie){
            printf("Koncowa weryfikacja: korekta liczby przecietych krawedzi z %d na %d\n", 
                   najlepszy_podzial->przeciete_krawedzie, weryfikacja);
            najlepszy_podzial->przeciete_krawedzie = weryfikacja;
        }
    }

    return najlepszy_podzial;
}