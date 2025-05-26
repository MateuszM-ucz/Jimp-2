#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "algorytm_kl.h"

/* struktura do przechowywania ruchu w algorytmie KL */
typedef struct {
    int wierzcholek;       /* wierzcholek do przeniesienia */
    int stara_czesc;       /* oryginalna czesc */
    int nowa_czesc;        /* docelowa czesc */
    int zysk;              /* zysk z przeniesienia */
} Ruch;

/* funkcja do obliczania liczby sasiadow wierzcholka w danej czesci */
int oblicz_liczbe_sasiadow(Graf* graf, Podzial* podzial, int wierzcholek, int czesc){
    int liczba = 0;
    
    for(int j = graf->wskazniki_listy[wierzcholek]; j < graf->wskazniki_listy[wierzcholek + 1]; j++){
        int sasiad = graf->lista_sasiedztwa[j];
        if(sasiad >= 0 && sasiad < graf->liczba_wierzcholkow && podzial->przypisania[sasiad] == czesc){
            liczba++;
        }
    }
    
    return liczba;
}

/* funkcja do obliczania zysku z przeniesienia wierzcholka do innej czesci */
int oblicz_zysk(Graf* graf, Podzial* podzial, int wierzcholek, int nowa_czesc){
    int obecna_czesc = podzial->przypisania[wierzcholek];
    
    /* liczba sasiadow w obecnej czesci */
    int sasiedzi_w_obecnej = oblicz_liczbe_sasiadow(graf, podzial, wierzcholek, obecna_czesc);
    
    /* liczba sasiadow w nowej czesci */
    int sasiedzi_w_nowej = oblicz_liczbe_sasiadow(graf, podzial, wierzcholek, nowa_czesc);
    
    /* zysk = (liczba sasiadow w nowej czesci) - (liczba sasiadow w obecnej czesci) */
    return sasiedzi_w_nowej - sasiedzi_w_obecnej;
}

/* funkcja wykonujaca jedno przejscie algorytmu Kernighana-Lina */
int przejscie_kernighan_lin(Graf* graf, Podzial* podzial){
    int liczba_wierzcholkow = graf->liczba_wierzcholkow;
    int liczba_czesci = podzial->liczba_czesci;
    
    if(liczba_wierzcholkow == 0 || liczba_czesci <= 1){
        return 0;
    }
    
    /* tymczasowe struktury */
    Ruch* ruchy = (Ruch*)malloc(liczba_wierzcholkow * sizeof(Ruch));
    int* kumulatywny_zysk = (int*)malloc((liczba_wierzcholkow + 1) * sizeof(int));
    int* przeniesione = (int*)calloc(liczba_wierzcholkow, sizeof(int));
    
    if(!ruchy || !kumulatywny_zysk || !przeniesione){
        fprintf(stderr, "Blad alokacji pamieci\n");
        if(ruchy) free(ruchy);
        if(kumulatywny_zysk) free(kumulatywny_zysk);
        if(przeniesione) free(przeniesione);
        return 0;
    }
    
    /* zapisz oryginalny stan podzialu */
    int* oryginalne_przypisania = (int*)malloc(liczba_wierzcholkow * sizeof(int));
    int* oryginalne_rozmiary = (int*)malloc(liczba_czesci * sizeof(int));
    
    if(!oryginalne_przypisania || !oryginalne_rozmiary){
        fprintf(stderr, "Blad alokacji pamieci\n");
        free(ruchy);
        free(kumulatywny_zysk);
        free(przeniesione);
        if(oryginalne_przypisania) free(oryginalne_przypisania);
        if(oryginalne_rozmiary) free(oryginalne_rozmiary);
        return 0;
    }
    
    memcpy(oryginalne_przypisania, podzial->przypisania, liczba_wierzcholkow * sizeof(int));
    memcpy(oryginalne_rozmiary, podzial->rozmiary_czesci, liczba_czesci * sizeof(int));
    
    /* inicjalizacja */
    kumulatywny_zysk[0] = 0;
    int liczba_ruchow = 0;
    
    /* sredni rozmiar czesci dla ograniczen rownowagi */
    int sredni_rozmiar = liczba_wierzcholkow / liczba_czesci;
    int max_nierownosc = (sredni_rozmiar * podzial->margines_procentowy) / 100;
    if(max_nierownosc < 1) max_nierownosc = 1;
    
    /* faza 1: znajdowanie sekwencji ruchow */
    for(int krok = 0; krok < liczba_wierzcholkow; krok++){
        int najlepszy_zysk = -1; /* szukamy zysku > 0 */
        int najlepszy_wierzcholek = -1;
        int najlepsza_docelowa_czesc = -1;
        
        /* sprawdz kazdy wierzcholek */
        for(int v = 0; v < liczba_wierzcholkow; v++){
            if(przeniesione[v]) continue; /* pomin juz przeniesione wierzcholki */
            
            int obecna_czesc = podzial->przypisania[v];
            
            /* sprawdz kazda mozliwa docelowa czesc */
            for(int docelowa_czesc = 0; docelowa_czesc < liczba_czesci; docelowa_czesc++){
                if(docelowa_czesc == obecna_czesc) continue; /* pomin obecna czesc */
                
                /* sprawdz ograniczenia rozmiaru czesci */
                if(podzial->rozmiary_czesci[docelowa_czesc] >= sredni_rozmiar + max_nierownosc ||
                    podzial->rozmiary_czesci[obecna_czesc] <= sredni_rozmiar - max_nierownosc){
                    continue; /* nie mozemy zwiekszyc tej czesci lub zmniejszyc obecnej */
                }
                
                /* oblicz zysk z przeniesienia */
                int zysk = oblicz_zysk(graf, podzial, v, docelowa_czesc);
                
                /* aktualizuj najlepszy ruch */
                if(zysk > najlepszy_zysk){
                    najlepszy_zysk = zysk;
                    najlepszy_wierzcholek = v;
                    najlepsza_docelowa_czesc = docelowa_czesc;
                }
            }
        }
        
        /* jesli nie znaleziono korzystnego ruchu, zakoncz */
        if(najlepszy_wierzcholek == -1 || najlepszy_zysk <= 0){
            break;
        }
        
        /* zapisz ruch */
        ruchy[liczba_ruchow].wierzcholek = najlepszy_wierzcholek;
        ruchy[liczba_ruchow].stara_czesc = podzial->przypisania[najlepszy_wierzcholek];
        ruchy[liczba_ruchow].nowa_czesc = najlepsza_docelowa_czesc;
        ruchy[liczba_ruchow].zysk = najlepszy_zysk;
        
        /* aktualizuj kumulatywny zysk */
        kumulatywny_zysk[liczba_ruchow + 1] = kumulatywny_zysk[liczba_ruchow] + najlepszy_zysk;
        liczba_ruchow++;
        
        /* tymczasowo przenies wierzcholek */
        int stara_czesc = podzial->przypisania[najlepszy_wierzcholek];
        podzial->przypisania[najlepszy_wierzcholek] = najlepsza_docelowa_czesc;
        podzial->rozmiary_czesci[stara_czesc]--;
        podzial->rozmiary_czesci[najlepsza_docelowa_czesc]++;
        
        /* oznacz jako przeniesiony */
        przeniesione[najlepszy_wierzcholek] = 1;
    }
    
    /* faza 2: znajdz prefiks z maksymalnym zyskiem */
    int max_kumulatywny_zysk = 0;
    int max_prefiks_idx = 0;
    
    for(int i = 1; i <= liczba_ruchow; i++){
        if(kumulatywny_zysk[i] > max_kumulatywny_zysk){
            max_kumulatywny_zysk = kumulatywny_zysk[i];
            max_prefiks_idx = i;
        }
    }
    
    /* faza 3: przywroc oryginalny stan i zastosuj tylko najlepszy prefiks ruchow */
    memcpy(podzial->przypisania, oryginalne_przypisania, liczba_wierzcholkow * sizeof(int));
    memcpy(podzial->rozmiary_czesci, oryginalne_rozmiary, liczba_czesci * sizeof(int));
    
    /* zastosuj tylko te ruchy, ktore tworza najlepszy prefiks */
    if(max_kumulatywny_zysk > 0){
        for(int i = 0; i < max_prefiks_idx; i++){
            int v = ruchy[i].wierzcholek;
            int stara_czesc = ruchy[i].stara_czesc;
            int nowa_czesc = ruchy[i].nowa_czesc;
            
            podzial->przypisania[v] = nowa_czesc;
            podzial->rozmiary_czesci[stara_czesc]--;
            podzial->rozmiary_czesci[nowa_czesc]++;
        }
        
        /* zaktualizuj liczbe przecietych krawedzi */
        podzial->przeciete_krawedzie -= max_kumulatywny_zysk;
    }
    
    /* zwolnij pamiec */
    free(ruchy);
    free(kumulatywny_zysk);
    free(przeniesione);
    free(oryginalne_przypisania);
    free(oryginalne_rozmiary);
    
    return max_kumulatywny_zysk > 0;
}

/* glowna funkcja algorytmu Kernighana-Lina */
void algorytm_kernighan_lin(Graf* graf, Podzial* podzial){
    if(!graf || !podzial || graf->liczba_wierzcholkow == 0 || podzial->liczba_czesci <= 1){
        return;
    }
    
    /* ustawienie parametrow */
    int max_iteracji = 50;
    if(graf->liczba_wierzcholkow > 5000){
        max_iteracji = 20;
        printf("Duzy graf wykryty (%d wierzcholkow) - ograniczenie do %d iteracji KL\n", 
               graf->liczba_wierzcholkow, max_iteracji);
    }
    
    printf("Poczatkowa liczba przecietych krawedzi: %d\n", podzial->przeciete_krawedzie);
    
    /* powtarzaj przejscia KL, az nie bedzie wiecej poprawy */
    int iteracja = 0;
    int poprawa = 1;
    
    while(poprawa && iteracja < max_iteracji){
        poprawa = przejscie_kernighan_lin(graf, podzial);
        iteracja++;
        
        if(poprawa){
            printf("Iteracja %d: Przeciete krawedzie = %d\n", iteracja, podzial->przeciete_krawedzie);
        }else{
            printf("Brak poprawy w iteracji %d, koniec algorytmu.\n", iteracja);
        }
    }
    
    /* zakonczenie algorytmu */
    printf("Wyjscie z algorytmu KL po %d iteracjach.\n", iteracja);
    printf("Koncowa liczba przecietych krawedzi: %d\n", podzial->przeciete_krawedzie);
    
    /* ostateczna weryfikacja liczby przecietych krawedzi */
    int final_edges = oblicz_przeciete_krawedzie(graf, podzial);
    if(final_edges != podzial->przeciete_krawedzie){
        printf("Korekta koncowej liczby przecietych krawedzi: zmiana z %d na %d\n", 
               podzial->przeciete_krawedzie, final_edges);
        podzial->przeciete_krawedzie = final_edges;
    }
}