#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "podzial.h"

/* oblicz liczbe przecietych krawedzi w podziale */
int oblicz_przeciete_krawedzie(Graf* graf, Podzial* podzial){
    if(!graf || !podzial || graf->liczba_wierzcholkow == 0){
        return 0;
    }
    
    int przeciete_krawedzie = 0;
    
    /* uzyjemy zestawu (v,u) gdzie v < u aby upewnic sie, ze kazda krawedz liczymy tylko raz */
    for(int u = 0; u < graf->liczba_wierzcholkow; u++){
        int czesc_u = podzial->przypisania[u];
        
        for(int j = graf->wskazniki_listy[u]; j < graf->wskazniki_listy[u + 1]; j++){
            int v = graf->lista_sasiedztwa[j];
            
            /* sprawdz poprawnosc indeksu */
            if(v < 0 || v >= graf->liczba_wierzcholkow){
                continue;
            }
            
            /* liczymy krawedz tylko gdy u < v aby uniknac podwojnego liczenia */
            if(u < v){
                int czesc_v = podzial->przypisania[v];
                
                if(czesc_u != czesc_v){
                    przeciete_krawedzie++;
                }
            }
        }
    }
    
    return przeciete_krawedzie;
}

/* inicjalizacja podzialu metoda modulo */
Podzial* inicjalizuj_podzial_modulo(Graf* graf, int liczba_czesci, int margines_procentowy){
    if(!graf || graf->liczba_wierzcholkow <= 0 || liczba_czesci <= 0){
        fprintf(stderr, "Nieprawidlowe parametry w inicjalizuj_podzial_modulo\n");
        return NULL;
    }
    
    Podzial* podzial = (Podzial*)malloc(sizeof(Podzial));
    if(!podzial){
        fprintf(stderr, "Blad alokacji pamieci\n");
        return NULL;
    }
    
    podzial->liczba_czesci = liczba_czesci;
    podzial->margines_procentowy = margines_procentowy;
    podzial->przypisania = (int*)malloc(graf->liczba_wierzcholkow * sizeof(int));
    podzial->rozmiary_czesci = (int*)calloc(liczba_czesci, sizeof(int));
    
    if(!podzial->przypisania || !podzial->rozmiary_czesci){
        fprintf(stderr, "Blad alokacji pamieci\n");
        free(podzial->przypisania);
        free(podzial->rozmiary_czesci);
        free(podzial);
        return NULL;
    }
    
    /* metoda modulo: wierzcholek i idzie do czesci i % liczba_czesci */
    for(int i = 0; i < graf->liczba_wierzcholkow; i++){
        int czesc = i % liczba_czesci;
        podzial->przypisania[i] = czesc;
        podzial->rozmiary_czesci[czesc]++;
    }
    
    /* oblicz poczatkowa liczbe przecietych krawedzi */
    podzial->przeciete_krawedzie = oblicz_przeciete_krawedzie(graf, podzial);
    
    return podzial;
}

/* inicjalizacja podzialu metoda sekwencyjna */
Podzial* inicjalizuj_podzial_sekwencyjny(Graf* graf, int liczba_czesci, int margines_procentowy){
    if(!graf || graf->liczba_wierzcholkow <= 0 || liczba_czesci <= 0){
        fprintf(stderr, "Nieprawidlowe parametry w inicjalizuj_podzial_sekwencyjny\n");
        return NULL;
    }
    
    Podzial* podzial = (Podzial*)malloc(sizeof(Podzial));
    if(!podzial){
        fprintf(stderr, "Blad alokacji pamieci\n");
        return NULL;
    }
    
    podzial->liczba_czesci = liczba_czesci;
    podzial->margines_procentowy = margines_procentowy;
    podzial->przypisania = (int*)malloc(graf->liczba_wierzcholkow * sizeof(int));
    podzial->rozmiary_czesci = (int*)calloc(liczba_czesci, sizeof(int));
    
    if(!podzial->przypisania || !podzial->rozmiary_czesci){
        fprintf(stderr, "Blad alokacji pamieci\n");
        free(podzial->przypisania);
        free(podzial->rozmiary_czesci);
        free(podzial);
        return NULL;
    }
    
    int wierzcholki_na_czesc = graf->liczba_wierzcholkow / liczba_czesci;
    int dodatkowe = graf->liczba_wierzcholkow % liczba_czesci;
    
    int indeks_wierzcholka = 0;
    for(int p = 0; p < liczba_czesci; p++){
        int rozmiar_czesci = wierzcholki_na_czesc + (p < dodatkowe ? 1 : 0);
        for(int i = 0; i < rozmiar_czesci && indeks_wierzcholka < graf->liczba_wierzcholkow; i++){
            podzial->przypisania[indeks_wierzcholka] = p;
            podzial->rozmiary_czesci[p]++;
            indeks_wierzcholka++;
        }
    }
    
    /* oblicz poczatkowa liczbe przecietych krawedzi */
    podzial->przeciete_krawedzie = oblicz_przeciete_krawedzie(graf, podzial);
    
    return podzial;
}

/* funkcja balansujaca losowy podzial */
void zbalansuj_losowy_podzial(Graf* graf, Podzial* podzial){
    if(!graf || !podzial || graf->liczba_wierzcholkow == 0 || podzial->liczba_czesci <= 1){
        return;
    }
    
    int sredni_rozmiar = graf->liczba_wierzcholkow / podzial->liczba_czesci;
    int max_nierownosc = (sredni_rozmiar * podzial->margines_procentowy) / 100;
    if(max_nierownosc < 1) max_nierownosc = 1;
    
    /* tablice do sledzenia czesci zbyt duzych i zbyt malych */
    int* parts_above = (int*)malloc(podzial->liczba_czesci * sizeof(int));
    int* parts_below = (int*)malloc(podzial->liczba_czesci * sizeof(int));
    
    if(!parts_above || !parts_below){
        fprintf(stderr, "Blad alokacji pamieci w zbalansuj_losowy_podzial\n");
        if(parts_above) free(parts_above);
        if(parts_below) free(parts_below);
        return;
    }
    
    /* znajdz czesci ktore sa zbyt duze lub zbyt male */
    int iteracje = 0;
    int max_iteracje = graf->liczba_wierzcholkow / 2; /* limit bezpieczenstwa */
    
    while(iteracje < max_iteracje){
        int above_count = 0, below_count = 0;
        
        /* znajdz czesci zbyt duze i zbyt male */
        for(int p = 0; p < podzial->liczba_czesci; p++){
            int current_size = podzial->rozmiary_czesci[p];
            
            if(current_size > sredni_rozmiar + max_nierownosc){
                parts_above[above_count++] = p;
            }else if(current_size < sredni_rozmiar - max_nierownosc){
                parts_below[below_count++] = p;
            }
        }
        
        /* jesli wszystko jest juz zbalansowane, konczymy */
        if(above_count == 0 || below_count == 0){
            break;
        }
        
        /* przenies wierzcholki z czesci zbyt duzych do zbyt malych */
        for(int i = 0; i < above_count && i < below_count; i++){
            int source_part = parts_above[i];
            int target_part = parts_below[i];
            
            /* znajdz losowy wierzcholek z source_part */
            int wybrane_wierzcholki = 0;
            int* candidates = (int*)malloc(graf->liczba_wierzcholkow * sizeof(int));
            
            if(!candidates){
                fprintf(stderr, "Blad alokacji pamieci dla candidates\n");
                break;
            }
            
            /* zbierz wszystkie wierzcholki z source_part */
            for(int v = 0; v < graf->liczba_wierzcholkow; v++){
                if(podzial->przypisania[v] == source_part){
                    candidates[wybrane_wierzcholki++] = v;
                }
            }
            
            if(wybrane_wierzcholki > 0){
                /* wybierz losowy wierzcholek */
                int idx = rand() % wybrane_wierzcholki;
                int v = candidates[idx];
                
                /* przenies wierzcholek */
                podzial->przypisania[v] = target_part;
                podzial->rozmiary_czesci[source_part]--;
                podzial->rozmiary_czesci[target_part]++;
            }
            
            free(candidates);
        }
        
        iteracje++;
    }
    
    free(parts_above);
    free(parts_below);
    
    /* zaktualizuj liczbe przecietych krawedzi */
    podzial->przeciete_krawedzie = oblicz_przeciete_krawedzie(graf, podzial);
}

/* inicjalizacja losowego podzialu */
Podzial* inicjalizuj_podzial_losowy(Graf* graf, int liczba_czesci, int margines_procentowy){
    if(!graf || graf->liczba_wierzcholkow <= 0 || liczba_czesci <= 0){
        fprintf(stderr, "Nieprawidlowe parametry w inicjalizuj_podzial_losowy\n");
        return NULL;
    }
    
    Podzial* podzial = (Podzial*)malloc(sizeof(Podzial));
    if(!podzial){
        fprintf(stderr, "Blad alokacji pamieci\n");
        return NULL;
    }
    
    podzial->liczba_czesci = liczba_czesci;
    podzial->margines_procentowy = margines_procentowy;
    podzial->przypisania = (int*)malloc(graf->liczba_wierzcholkow * sizeof(int));
    podzial->rozmiary_czesci = (int*)calloc(liczba_czesci, sizeof(int));
    
    if(!podzial->przypisania || !podzial->rozmiary_czesci){
        fprintf(stderr, "Blad alokacji pamieci\n");
        free(podzial->przypisania);
        free(podzial->rozmiary_czesci);
        free(podzial);
        return NULL;
    }
    
    /* losowo przypisz kazdy wierzcholek do czesci */
    for(int i = 0; i < graf->liczba_wierzcholkow; i++){
        int czesc = rand() % liczba_czesci;
        podzial->przypisania[i] = czesc;
        podzial->rozmiary_czesci[czesc]++;
    }
    
    /* balansuj podzial */
    zbalansuj_losowy_podzial(graf, podzial);
    
    return podzial;
}

/* funkcja kopiujaca podzial */
Podzial* kopiuj_podzial(Podzial* zrodlo, int liczba_wierzcholkow){
    if(!zrodlo || liczba_wierzcholkow <= 0){
        fprintf(stderr, "Nieprawidlowe parametry w kopiuj_podzial\n");
        return NULL;
    }
    
    Podzial* kopia = (Podzial*)malloc(sizeof(Podzial));
    if(!kopia){
        fprintf(stderr, "Blad alokacji pamieci\n");
        return NULL;
    }
    
    kopia->liczba_czesci = zrodlo->liczba_czesci;
    kopia->margines_procentowy = zrodlo->margines_procentowy;
    kopia->przeciete_krawedzie = zrodlo->przeciete_krawedzie;
    
    kopia->przypisania = (int*)malloc(liczba_wierzcholkow * sizeof(int));
    kopia->rozmiary_czesci = (int*)malloc(kopia->liczba_czesci * sizeof(int));
    
    if(!kopia->przypisania || !kopia->rozmiary_czesci){
        fprintf(stderr, "Blad alokacji pamieci\n");
        free(kopia->przypisania);
        free(kopia->rozmiary_czesci);
        free(kopia);
        return NULL;
    }
    
    memcpy(kopia->przypisania, zrodlo->przypisania, liczba_wierzcholkow * sizeof(int));
    memcpy(kopia->rozmiary_czesci, zrodlo->rozmiary_czesci, kopia->liczba_czesci * sizeof(int));
    
    return kopia;
}

/* funkcja do wyswietlania informacji o podziale */
void wyswietl_podzial(Graf* graf, Podzial* podzial){
    if(!graf || !podzial){
        fprintf(stderr, "Nieprawidlowe parametry w wyswietl_podzial\n");
        return;
    }
    
    printf("Podzial na %d czesci:\n", podzial->liczba_czesci);
    
    for(int p = 0; p < podzial->liczba_czesci; p++){
        printf("Czesc %d (rozmiar %d): ", p, podzial->rozmiary_czesci[p]);
        
        /* wyswietl do 10 wierzcholkow z kazdej czesci dla lepszej czytelnosci */
        int licznik = 0;
        for(int v = 0; v < graf->liczba_wierzcholkow && licznik < 10; v++){
            if(podzial->przypisania[v] == p){
                printf("%d ", v);
                licznik++;
            }
        }
        
        if(podzial->rozmiary_czesci[p] > 10){
            printf("... (i %d wiecej)", podzial->rozmiary_czesci[p] - 10);
        }
        printf("\n");
    }
    
    printf("Laczna liczba przecietych krawedzi: %d\n", podzial->przeciete_krawedzie);
}

/* zapisz podzial do pliku wyjsciowego */
void zapisz_podzial(const char* nazwa_pliku, Graf* graf, Podzial* podzial, const char* format){
    if(!nazwa_pliku || !graf || !podzial || !format){
        fprintf(stderr, "Nieprawidlowe parametry w zapisz_podzial\n");
        return;
    }
    
    /* sprawdz rozszerzenie pliku wyjsciowego */
    const char* rozszerzenie = strrchr(nazwa_pliku, '.');
    int format_binarny = 0;
    
    /* jesli rozszerzenie to .bin lub format jest ustawiony na bin, uzywamy formatu binarnego */
    if((rozszerzenie && strcmp(rozszerzenie, ".bin") == 0) || strcmp(format, "bin") == 0){
        format_binarny = 1;
    }
    
    /* wywolaj odpowiednia funkcje zapisu */
    if(format_binarny){
        zapisz_podzial_binarny(nazwa_pliku, graf, podzial);
    }else{
        zapisz_podzial_tekstowy(nazwa_pliku, graf, podzial);
    }
}

void zapisz_podzial_tekstowy(const char* nazwa_pliku, Graf* graf, Podzial* podzial){
    if(!nazwa_pliku || !graf || !podzial){
        fprintf(stderr, "Nieprawidlowe parametry w zapisz_podzial_tekstowy\n");
        return;
    }
    
    FILE* plik = fopen(nazwa_pliku, "w");
    if(!plik){
        fprintf(stderr, "Blad tworzenia pliku wyjsciowego: %s\n", nazwa_pliku);
        return;
    }
    
    /* 1. Naglowek z informacjami o podziale */
    fprintf(plik, "# Podzial grafu na %d czesci\n", podzial->liczba_czesci);
    fprintf(plik, "# Liczba wierzcholkow: %d\n", graf->liczba_wierzcholkow);
    fprintf(plik, "# Liczba przecietych krawedzi: %d\n", podzial->przeciete_krawedzie);
    
    /* 2. Macierz sasiedztwa */
    fprintf(plik, "\n# Macierz sasiedztwa:\n");
    
    /* alokuj pamiec dla macierzy */
    int liczba_wierzcholkow = graf->liczba_wierzcholkow;
    int* macierz_row = (int*)calloc(liczba_wierzcholkow, sizeof(int));
    
    if(!macierz_row){
        fprintf(stderr, "Blad alokacji pamieci dla macierzy sasiedztwa\n");
        fclose(plik);
        return;
    }
    
    /* dla kazdego wierzcholka */
    for(int i = 0; i < liczba_wierzcholkow; i++){
        /* wyzeruj wiersz macierzy */
        memset(macierz_row, 0, liczba_wierzcholkow * sizeof(int));
        
        /* zaznacz sasiadow */
        for(int j = graf->wskazniki_listy[i]; j < graf->wskazniki_listy[i + 1]; j++){
            int sasiad = graf->lista_sasiedztwa[j];
            if(sasiad >= 0 && sasiad < liczba_wierzcholkow){
                macierz_row[sasiad] = 1;
            }
        }
        
        /* zapisz wiersz macierzy */
        fprintf(plik, "[");
        for(int j = 0; j < liczba_wierzcholkow; j++){
            fprintf(plik, "%d.", macierz_row[j]);
            if(j < liczba_wierzcholkow - 1){
                fprintf(plik, " ");
            }
        }
        fprintf(plik, "]\n");
    }
    
    /* 3. Lista przypisan */
    fprintf(plik, "\n# Lista przypisan wierzcholkow do czesci:\n");
    fprintf(plik, "# Format: <id_wierzcholka> - <id_czesci>\n");
    
    for(int v = 0; v < liczba_wierzcholkow; v++){
        fprintf(plik, "%d - %d\n", v, podzial->przypisania[v]);
    }
    
    /* zwolnij pamiec i zamknij plik */
    free(macierz_row);
    fclose(plik);
    
    printf("Podzial zapisany do %s w formacie tekstowym zgodnym z dokumentacja\n", nazwa_pliku);
}

void zapisz_podzial_binarny(const char* nazwa_pliku, Graf* graf, Podzial* podzial){
    if(!nazwa_pliku || !graf || !podzial){
        fprintf(stderr, "Nieprawidlowe parametry w zapisz_podzial_binarny\n");
        return;
    }
    
    FILE* plik = fopen(nazwa_pliku, "w");
    if(!plik){
        fprintf(stderr, "Blad tworzenia pliku wyjsciowego: %s\n", nazwa_pliku);
        return;
    }
    
    /* linia 1: liczba czesci w podziale (liczba encji w strukturze dodatkowej) */
    fprintf(plik, "%d\n", podzial->liczba_czesci);
    
    /* linia 2: wierzcholki przypisane do poszczegolnych czesci (wartosci struktury dodatkowej) */
    /* tworzymy liste wierzcholkow dla kazdej czesci */
    int** wierzcholki_czesci = (int**)malloc(podzial->liczba_czesci * sizeof(int*));
    int* rozmiary_list = (int*)calloc(podzial->liczba_czesci, sizeof(int));
    
    if(!wierzcholki_czesci || !rozmiary_list){
        fprintf(stderr, "Blad alokacji pamieci w zapisz_podzial_binarny\n");
        if(wierzcholki_czesci) free(wierzcholki_czesci);
        if(rozmiary_list) free(rozmiary_list);
        fclose(plik);
        return;
    }
    
    /* inicjalizacja tablic */
    for(int i = 0; i < podzial->liczba_czesci; i++){
        wierzcholki_czesci[i] = (int*)malloc(podzial->rozmiary_czesci[i] * sizeof(int));
        if(!wierzcholki_czesci[i]){
            fprintf(stderr, "Blad alokacji pamieci dla listy wierzcholkow czesci %d\n", i);
            for(int j = 0; j < i; j++){
                free(wierzcholki_czesci[j]);
            }
            free(wierzcholki_czesci);
            free(rozmiary_list);
            fclose(plik);
            return;
        }
    }
    
    /* przydziel wierzcholki do odpowiednich list */
    for(int v = 0; v < graf->liczba_wierzcholkow; v++){
        int czesc = podzial->przypisania[v];
        if(czesc >= 0 && czesc < podzial->liczba_czesci){
            wierzcholki_czesci[czesc][rozmiary_list[czesc]++] = v;
        }
    }
    
    /* zapisz dane wierzcholkow do pliku (linia 2) */
    for(int i = 0; i < podzial->liczba_czesci; i++){
        for(int j = 0; j < rozmiary_list[i]; j++){
            fprintf(plik, "%d", wierzcholki_czesci[i][j]);
            /* dodaj srednik po kazdej wartosci oprocz ostatniej */
            if(!(i == podzial->liczba_czesci - 1 && j == rozmiary_list[i] - 1)){
                fprintf(plik, ";");
            }
        }
    }
    fprintf(plik, "\n");
    
    /* linia 3: wskazniki wierszy dla struktury czesci (wskazniki wierszy struktury dodatkowej) */
    int wskaznik = 0;
    for(int i = 0; i <= podzial->liczba_czesci; i++){
        fprintf(plik, "%d", wskaznik);
        if(i < podzial->liczba_czesci){
            fprintf(plik, ";");
            wskaznik += rozmiary_list[i];
        }
    }
    fprintf(plik, "\n");
    
    /* linia 4: lista sasiedztwa grafu (lista sasiadow glownego grafu) */
    int liczba_elementow_sasiedztwa = graf->wskazniki_listy[graf->liczba_wierzcholkow];
    for(int i = 0; i < liczba_elementow_sasiedztwa; i++){
        fprintf(plik, "%d", graf->lista_sasiedztwa[i]);
        if(i < liczba_elementow_sasiedztwa - 1){
            fprintf(plik, ";");
        }
    }
    fprintf(plik, "\n");
    
    /* linia 5: wskazniki wierszy grafu glownego (wskazniki wierszy glownego grafu) */
    /* w przypadku jednego grafu, zapisujemy tylko oryginalne wskazniki */
    for(int i = 0; i <= graf->liczba_wierzcholkow; i++){
        fprintf(plik, "%d", graf->wskazniki_listy[i]);
        if(i < graf->liczba_wierzcholkow){
            fprintf(plik, ";");
        }
    }
    fprintf(plik, "\n");
    
    /* linia 6 i dalej: wskazniki dla grafow czesci (jesli traktujemy kazda czesc jako osobny podgraf) */
    /* dla kazdej czesci tworzymy osobny graf z wlasnymi wskaznikami */
    for(int p = 0; p < podzial->liczba_czesci; p++){
        /* obliczamy liczbe wierzcholkow w tej czesci */
        int liczba_wierzcholkow_w_czesci = podzial->rozmiary_czesci[p];
        
        /* tworzymy wskazniki dla tej czesci */
        int* wskazniki_czesci = (int*)malloc((liczba_wierzcholkow_w_czesci + 1) * sizeof(int));
        if(!wskazniki_czesci){
            fprintf(stderr, "Blad alokacji pamieci dla wskaznikow czesci %d\n", p);
            /* zwalnianie zasobow */
            for(int i = 0; i < podzial->liczba_czesci; i++){
                free(wierzcholki_czesci[i]);
            }
            free(wierzcholki_czesci);
            free(rozmiary_list);
            fclose(plik);
            return;
        }
        
        /* inicjalizacja wskaznikow */
        wskazniki_czesci[0] = 0;
        
        /* dla kazdego wierzcholka w tej czesci */
        for(int v_idx = 0; v_idx < liczba_wierzcholkow_w_czesci; v_idx++){
            int wierzcholek = wierzcholki_czesci[p][v_idx];
            
            /* obliczamy liczbe sasiadow, ktorzy rowniez naleza do tej samej czesci */
            int liczba_sasiadow_w_czesci = 0;
            for(int j = graf->wskazniki_listy[wierzcholek]; j < graf->wskazniki_listy[wierzcholek + 1]; j++){
                int sasiad = graf->lista_sasiedztwa[j];
                if(sasiad >= 0 && sasiad < graf->liczba_wierzcholkow && podzial->przypisania[sasiad] == p){
                    liczba_sasiadow_w_czesci++;
                }
            }
            
            /* aktualizujemy wskaznik dla nastepnego wierzcholka */
            wskazniki_czesci[v_idx + 1] = wskazniki_czesci[v_idx] + liczba_sasiadow_w_czesci;
        }
        
        /* zapisujemy wskazniki dla tej czesci */
        for(int i = 0; i <= liczba_wierzcholkow_w_czesci; i++){
            fprintf(plik, "%d", wskazniki_czesci[i]);
            if(i < liczba_wierzcholkow_w_czesci){
                fprintf(plik, ";");
            }
        }
        fprintf(plik, "\n");
        
        free(wskazniki_czesci);
    }
    
    /* zwolnij pamiec */
    for(int i = 0; i < podzial->liczba_czesci; i++){
        free(wierzcholki_czesci[i]);
    }
    free(wierzcholki_czesci);
    free(rozmiary_list);
    
    fclose(plik);
    
    printf("Podzial zapisany do %s w formacie CSRRG\n", nazwa_pliku);
}
/*
void zapisz_podzial_binarny(const char* nazwa_pliku, Graf* graf, Podzial* podzial) {
    if (!nazwa_pliku || !graf || !podzial) {
        fprintf(stderr, "Nieprawidlowe parametry w zapisz_podzial_binarny\n");
        return;
    }
    
    FILE* plik = fopen(nazwa_pliku, "w");
    if (!plik) {
        fprintf(stderr, "Błąd tworzenia pliku wyjściowego: %s\n", nazwa_pliku);
        return;
    }
    
    // Linia 1: Liczba części w podziale (liczba encji w strukturze dodatkowej)
    fprintf(plik, "%d\n", podzial->liczba_czesci);
    
    // Linia 2: Wierzchołki przypisane do poszczególnych części (wartości struktury dodatkowej)
    // Tworzymy listę wierzchołków dla każdej części
    int** wierzcholki_czesci = (int**)malloc(podzial->liczba_czesci * sizeof(int*));
    int* rozmiary_list = (int*)calloc(podzial->liczba_czesci, sizeof(int));
    
    if (!wierzcholki_czesci || !rozmiary_list) {
        fprintf(stderr, "Błąd alokacji pamięci w zapisz_podzial_binarny\n");
        if (wierzcholki_czesci) free(wierzcholki_czesci);
        if (rozmiary_list) free(rozmiary_list);
        fclose(plik);
        return;
    }
    
    // Inicjalizacja tablic
    for (int i = 0; i < podzial->liczba_czesci; i++) {
        wierzcholki_czesci[i] = (int*)malloc(podzial->rozmiary_czesci[i] * sizeof(int));
        if (!wierzcholki_czesci[i]) {
            fprintf(stderr, "Błąd alokacji pamięci dla listy wierzchołków części %d\n", i);
            for (int j = 0; j < i; j++) {
                free(wierzcholki_czesci[j]);
            }
            free(wierzcholki_czesci);
            free(rozmiary_list);
            fclose(plik);
            return;
        }
    }
    
    // Przydziel wierzcholki do odpowiednich list
    for (int v = 0; v < graf->liczba_wierzcholkow; v++) {
        int czesc = podzial->przypisania[v];
        if (czesc >= 0 && czesc < podzial->liczba_czesci) {
            wierzcholki_czesci[czesc][rozmiary_list[czesc]++] = v;
        }
    }
    
    // Zapisz dane wierzchołków do pliku (Linia 2)
    for (int i = 0; i < podzial->liczba_czesci; i++) {
        for (int j = 0; j < rozmiary_list[i]; j++) {
            fprintf(plik, "%d", wierzcholki_czesci[i][j]);
            // Dodaj średnik po każdej wartości oprócz ostatniej
            if (!(i == podzial->liczba_czesci - 1 && j == rozmiary_list[i] - 1)) {
                fprintf(plik, ";");
            }
        }
    }
    fprintf(plik, "\n");
    
    // Linia 3: Wskaźniki wierszy dla struktury części (wskaźniki wierszy struktury dodatkowej)
    int wskaznik = 0;
    for (int i = 0; i <= podzial->liczba_czesci; i++) {
        fprintf(plik, "%d", wskaznik);
        if (i < podzial->liczba_czesci) {
            fprintf(plik, ";");
            wskaznik += rozmiary_list[i];
        }
    }
    fprintf(plik, "\n");
    
    // Linia 4: Lista sąsiedztwa grafu (lista sąsiadów głównego grafu)
    int liczba_elementow_sasiedztwa = graf->wskazniki_listy[graf->liczba_wierzcholkow];
    for (int i = 0; i < liczba_elementow_sasiedztwa; i++) {
        fprintf(plik, "%d", graf->lista_sasiedztwa[i]);
        if (i < liczba_elementow_sasiedztwa - 1) {
            fprintf(plik, ";");
        }
    }
    fprintf(plik, "\n");
    
    // Linia 5: Wskaźniki wierszy grafu głównego (wskaźniki wierszy głównego grafu)
    // W przypadku jednego grafu, zapisujemy tylko oryginalne wskaźniki
    for (int i = 0; i <= graf->liczba_wierzcholkow; i++) {
        fprintf(plik, "%d", graf->wskazniki_listy[i]);
        if (i < graf->liczba_wierzcholkow) {
            fprintf(plik, ";");
        }
    }
    fprintf(plik, "\n");
    
    // Linia 6 i dalej: Wskaźniki dla grafów części (jeśli traktujemy każdą część jako osobny podgraf)
    // Dla każdej części tworzymy osobny graf z własnymi wskaźnikami
    for (int p = 0; p < podzial->liczba_czesci; p++) {
        // Obliczamy liczbę wierzchołków w tej części
        int liczba_wierzcholkow_w_czesci = podzial->rozmiary_czesci[p];
        
        // Tworzymy wskaźniki dla tej części
        int* wskazniki_czesci = (int*)malloc((liczba_wierzcholkow_w_czesci + 1) * sizeof(int));
        if (!wskazniki_czesci) {
            fprintf(stderr, "Błąd alokacji pamięci dla wskaźników części %d\n", p);
            // Zwalnianie zasobów
            for (int i = 0; i < podzial->liczba_czesci; i++) {
                free(wierzcholki_czesci[i]);
            }
            free(wierzcholki_czesci);
            free(rozmiary_list);
            fclose(plik);
            return;
        }
        
        // Inicjalizacja wskazikow
        wskazniki_czesci[0] = 0;
        
        // Dla każdego wierzchołka w tej części
        for (int v_idx = 0; v_idx < liczba_wierzcholkow_w_czesci; v_idx++) {
            int wierzcholek = wierzcholki_czesci[p][v_idx];
            
            // Obliczamy liczbę sąsiadów, którzy również należą do tej samej części
            int liczba_sasiadow_w_czesci = 0;
            for (int j = graf->wskazniki_listy[wierzcholek]; j < graf->wskazniki_listy[wierzcholek + 1]; j++) {
                int sasiad = graf->lista_sasiedztwa[j];
                if (sasiad >= 0 && sasiad < graf->liczba_wierzcholkow && podzial->przypisania[sasiad] == p) {
                    liczba_sasiadow_w_czesci++;
                }
            }
            
            // Aktualizujemy wskaźnik dla następnego wierzchołka
            wskazniki_czesci[v_idx + 1] = wskazniki_czesci[v_idx] + liczba_sasiadow_w_czesci;
        }
        
        // Zapisujemy wskaźniki dla tej części
        for (int i = 0; i <= liczba_wierzcholkow_w_czesci; i++) {
            fprintf(plik, "%d", wskazniki_czesci[i]);
            if (i < liczba_wierzcholkow_w_czesci) {
                fprintf(plik, ";");
            }
        }
        fprintf(plik, "\n");
        
        free(wskazniki_czesci);
    }
    
    // Zwolnij pamiec
    for (int i = 0; i < podzial->liczba_czesci; i++) {
        free(wierzcholki_czesci[i]);
    }
    free(wierzcholki_czesci);
    free(rozmiary_list);
    
    fclose(plik);
    
    printf("Podzial zapisany do %s w formacie CSRRG\n", nazwa_pliku);
}
*/

/* zwolnij pamiec uzywana przez podzial */
void zwolnij_podzial(Podzial* podzial){
    if(podzial){
        if(podzial->przypisania) free(podzial->przypisania);
        if(podzial->rozmiary_czesci) free(podzial->rozmiary_czesci);
        free(podzial);
    }
}