#define _CRT_SECURE_NO_WARNINGS /* wylacza ostrzezenia zwiazane z funkcjami uwazanymi za niezabezpieczone */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "graf.h"
#include "podzial.h"
#include "algorytm_kl.h"
#include "algorytm_hybrydowy.h"
#include "utils.h"

int main(int argc, char* argv[]){
    char* plik_wejsciowy = NULL;
    char* plik_wyjsciowy = NULL;
    int liczba_czesci = 2;  /* domyslna liczba czesci */
    int margines_procentowy = 10;  /* domyslny margines procentowy */
    char* format = "txt";  /* domyslny format wyjscia */
    char* typ_algorytmu = "modulo";  /* domyslny typ algorytmu podzialu */
    int uzyj_hybrydowy = 0;  /* domyslnie nie uzywaj algorytmu hybrydowego */
    
    /* inicjalizacja generatora liczb losowych dla algorytmu losowego */
    srand((unsigned int)time(NULL));
    
    /* analizuj argumenty wiersza polecen */
    parsuj_argumenty(argc, argv, &plik_wejsciowy, &plik_wyjsciowy, 
                    &liczba_czesci, &margines_procentowy, &format, &typ_algorytmu, &uzyj_hybrydowy);
    
    /* sprawdz czy wymagane argumenty zostaly podane */
    if(!plik_wejsciowy || !plik_wyjsciowy){
        fprintf(stderr, "Brak wymaganych argumentow.\n");
        wyswietl_uzycie(argv[0]);
        return EXIT_FAILURE;
    }
    
    /* sprawdz poprawnosc argumentow */
    if(liczba_czesci <= 0){
        fprintf(stderr, "Liczba czesci musi byc dodatnia\n");
        return EXIT_FAILURE;
    }
    
    if(margines_procentowy < 0 || margines_procentowy > 100){
        fprintf(stderr, "Margines procentowy musi byc pomiedzy 0 a 100\n");
        return EXIT_FAILURE;
    }
    
    if(strcmp(format, "txt") != 0 && strcmp(format, "bin") != 0){
        fprintf(stderr, "Niepoprawny format wyjscia. Musi byc 'txt' lub 'bin'\n");
        return EXIT_FAILURE;
    }
    
    /* automatycznie okresL format wyjsciowy na podstawie rozszerzenia pliku */
    const char* rozszerzenie_wyjscia = strrchr(plik_wyjsciowy, '.');
    if(rozszerzenie_wyjscia){
        if(strcmp(rozszerzenie_wyjscia, ".bin") == 0){
            format = "bin";
            printf("Wykryto format wyjsciowy: binarny (.bin)\n");
        }else if(strcmp(rozszerzenie_wyjscia, ".txt") == 0){
            format = "txt";
            printf("Wykryto format wyjsciowy: tekstowy (.txt)\n");
        }
    }
    
    /* sprawdz czy plik wejsciowy istnieje, a jesli nie, probuj znalezc alternatywe */
    char alt_plik[1024] = {0};
    FILE* test = fopen(plik_wejsciowy, "r");
    if(!test){
        /* plik nie istnieje - sprobuj zmienic rozszerzenie */
        const char* rozszerzenie = strrchr(plik_wejsciowy, '.');
        if(rozszerzenie){
            strncpy(alt_plik, plik_wejsciowy, sizeof(alt_plik) - 1);
            alt_plik[sizeof(alt_plik) - 1] = '\0'; /* upewnij sie ze jest zakonczony zerem */
            
            char* alt_rozszerzenie = strrchr(alt_plik, '.');
            if(alt_rozszerzenie){
                if(strcmp(rozszerzenie, ".csrrg") == 0){
                    /* sprobuj z rozszerzeniem .txt */
                    strcpy(alt_rozszerzenie, ".txt");
                }else{
                    /* sprobuj z rozszerzeniem .csrrg */
                    strcpy(alt_rozszerzenie, ".csrrg");
                }
                
                test = fopen(alt_plik, "r");
                if(test){
                    printf("Nie znaleziono pliku %s, zamiast tego uzywam %s\n", plik_wejsciowy, alt_plik);
                    plik_wejsciowy = strdup(alt_plik);
                    fclose(test);
                }
            }
        }
    }else{
        fclose(test);
    }
    
    /* wczytaj graf */
    printf("Wczytywanie grafu z pliku %s...\n", plik_wejsciowy);
    
    /* sprawdz czy to plik tekstowy czy binarny */
    const char* rozszerzenie = strrchr(plik_wejsciowy, '.');
    if(rozszerzenie && strcmp(rozszerzenie, ".csrrg") == 0){
        printf("Wykryto format wejsciowy: binarny CSR (.csrrg)\n");
    }else if(rozszerzenie && strcmp(rozszerzenie, ".txt") == 0){
        printf("Wykryto format wejsciowy: tekstowy (.txt)\n");
    }else{
        printf("Nieznany format wejsciowy, sprobuję odczytac jako plik tekstowy\n");
    }
    
    /* pomiar czasu wykonania */
    clock_t start_time = clock();
    
    Graf* graf = wczytaj_graf(plik_wejsciowy);
    if(!graf){
        fprintf(stderr, "Nie udalo sie wczytac grafu z %s\n", plik_wejsciowy);
        fprintf(stderr, "Sprawdz czy plik istnieje i ma odpowiedni format\n");
        return EXIT_FAILURE;
    }
    
    printf("Wczytano graf z %d wierzcholkami i %d krawedziami\n", 
           graf->liczba_wierzcholkow, graf->liczba_krawedzi);
    printf("Pamiec szacowana dla grafu: okolo %.2f MB\n", 
           (float)(graf->liczba_wierzcholkow + graf->wskazniki_listy[graf->liczba_wierzcholkow]) 
           * sizeof(int) / (1024*1024));
    
    /* podzial grafu */
    Podzial* podzial = NULL;
    
    if(uzyj_hybrydowy){
        /* uzyj algorytmu hybrydowego */
        printf("Uzywanie algorytmu hybrydowego do znalezienia optymalnego podzialu...\n");
        podzial = znajdz_najlepszy_podzial(graf, liczba_czesci, margines_procentowy);
    }else{
        /* uzyj pojedynczego algorytmu podzialu */
        if(strcmp(typ_algorytmu, "modulo") == 0){
            printf("Tworzenie podzialu metoda modulo...\n");
            podzial = inicjalizuj_podzial_modulo(graf, liczba_czesci, margines_procentowy);
        }else if(strcmp(typ_algorytmu, "sekwencyjny") == 0){
            printf("Tworzenie podzialu metoda sekwencyjna...\n");
            podzial = inicjalizuj_podzial_sekwencyjny(graf, liczba_czesci, margines_procentowy);
        }else if(strcmp(typ_algorytmu, "losowy") == 0){
            printf("Tworzenie podzialu metoda losowa...\n");
            podzial = inicjalizuj_podzial_losowy(graf, liczba_czesci, margines_procentowy);
        }else{
            fprintf(stderr, "Nieznany typ algorytmu: %s. Uzywam metody modulo.\n", typ_algorytmu);
            podzial = inicjalizuj_podzial_modulo(graf, liczba_czesci, margines_procentowy);
        }
        
        if(podzial){
            printf("Poczatkowy podzial ma %d przecietych krawedzi\n", podzial->przeciete_krawedzie);
            
            /* zastosuj algorytm Kernighana-Lina do optymalizacji podzialu */
            printf("Optymalizacja podzialu algorytmem Kernighana-Lina...\n");
            algorytm_kernighan_lin(graf, podzial);
            
            printf("Po optymalizacji podzial ma %d przecietych krawedzi\n", podzial->przeciete_krawedzie);
        }
    }
    
    if(!podzial){
        fprintf(stderr, "Nie udalo sie utworzyc podzialu\n");
        zwolnij_graf(graf);
        return EXIT_FAILURE;
    }

    /* oblicz calkowity czas wykonania */
    clock_t end_time = clock();
    double cpu_time_used = ((double) (end_time - start_time)) / CLOCKS_PER_SEC;
    printf("Czas wykonania: %.2f sekund\n", cpu_time_used);

    /* wyswietl informacje o podziale */
    wyswietl_podzial(graf, podzial);

    /* zapisz podzial do pliku wyjsciowego */
    zapisz_podzial(plik_wyjsciowy, graf, podzial, format);
    
    /* zwolnij pamiec */
    zwolnij_podzial(podzial);
    zwolnij_graf(graf);
    
    /* zwolnij pamiec zaalokowana przez strdup, jesli bylo uzywane */
    if(plik_wejsciowy != argv[1] && plik_wejsciowy != NULL){
        free(plik_wejsciowy);
    }
    
    return EXIT_SUCCESS;
}