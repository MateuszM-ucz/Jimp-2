#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "utils.h"

/* wyswietl instrukcje uzycia */
void wyswietl_uzycie(const char* nazwa_programu){
    const char* rzeczywista_nazwa = nazwa_programu;
    
    /* jesli podana jest pelna sciezka, znajdz tylko nazwe pliku */
    const char* ostatni_slash = strrchr(nazwa_programu, '/');
    if(ostatni_slash){
        rzeczywista_nazwa = ostatni_slash + 1;
    }else{
        ostatni_slash = strrchr(nazwa_programu, '\\');
        if(ostatni_slash){
            rzeczywista_nazwa = ostatni_slash + 1;
        }
    }
    
    printf("Uzycie: %s -i <plik_wejsciowy> [-p <liczba_czesci>] [-m <margines_procentowy>] [-f <format_wyjscia>] -o <plik_wyjsciowy> [-a <algorytm>] [-h]\n", rzeczywista_nazwa);
    printf("Opcje:\n");
    printf("  -i <plik_wejsciowy>      Sciezka do pliku wejsciowego (format .csrrg lub tekstowy CSR)\n");
    printf("  -p <liczba_czesci>       Liczba czesci, na ktore nalezy podzielic graf (domyslnie: 2)\n");
    printf("  -m <margines_procentowy> Maksymalny procentowy margines nierownosci miedzy czesciami (domyslnie: 10%%)\n");
    printf("  -f <format_wyjscia>      Format wyjscia - 'txt' lub 'bin' (domyslnie: txt)\n");
    printf("  -o <plik_wyjsciowy>      Sciezka do pliku wyjsciowego\n");
    printf("  -a <algorytm>            Algorytm podzialu - 'modulo', 'sekwencyjny' lub 'losowy' (domyslnie: modulo)\n");
    printf("  -y                       Uzyj algorytmu hybrydowego (testuje rozne strategie i wybiera najlepsza)\n");
    printf("  -h                       Wyswietl te instrukcje\n");
    printf("\n");
    printf("Uwagi:\n");
    printf("  - Format pliku wejsciowego jest automatycznie wykrywany na podstawie rozszerzenia\n");
    printf("    (.csrrg dla formatu binarnego CSR)\n");
    printf("  - Format pliku wyjsciowego moze byc okreslony przez rozszerzenie (.bin lub .txt)\n");
    printf("    lub jawnie za pomoca opcji -f\n");
    printf("  - Gdy uzywany jest algorytm hybrydowy (-y), opcja -a jest ignorowana\n");
}

/* sprawdz czy string zawiera liczbe calkowita */
int jest_liczba_calkowita(const char* str){
    if(!str || *str == '\0'){
        return 0;
    }
    
    /* pomijanie bialych znakow na poczatku */
    while(isspace((unsigned char)*str)){
        str++;
    }
    
    /* sprawdzenie znaku */
    if(*str == '+' || *str == '-'){
        str++;
    }
    
    /* wymagamy przynajmniej jednej cyfry */
    int ma_cyfre = 0;
    
    while(*str){
        if(!isdigit((unsigned char)*str)){
            /* pomijanie bialych znakow na koncu */
            while(isspace((unsigned char)*str)){
                str++;
            }
            /* jesli po cyfrach sa jeszcze jakies znaki, to nie jest to poprawna liczba */
            return *str == '\0' && ma_cyfre;
        }
        ma_cyfre = 1;
        str++;
    }
    
    return ma_cyfre;
}

/* niestandardowa funkcja do analizy argumentow wiersza polecen bez getopt.h */
void parsuj_argumenty(int argc, char* argv[], char** plik_wejsciowy, char** plik_wyjsciowy, 
                     int* liczba_czesci, int* margines_procentowy, char** format, 
                     char** typ_algorytmu, int* uzyj_hybrydowy){
    if(!plik_wejsciowy || !plik_wyjsciowy || !liczba_czesci || !margines_procentowy || 
        !format || !typ_algorytmu || !uzyj_hybrydowy){
        fprintf(stderr, "Blad: Nieprawidlowe wskazniki w funkcji parsuj_argumenty\n");
        return;
    }
    
    *plik_wejsciowy = NULL;
    *plik_wyjsciowy = NULL;
    *liczba_czesci = 2;  /* domyslna liczba czesci */
    *margines_procentowy = 10;  /* domyslny margines procentowy */
    *format = "txt";  /* domyslny format wyjscia */
    *typ_algorytmu = "modulo";  /* domyslny typ algorytmu */
    *uzyj_hybrydowy = 0;  /* domyslnie nie uzywaj algorytmu hybrydowego */
    
    /* poczatek analizy - sprawdz czy pierwszy argument nie jest opcja 
       (moze to byc przypadek gdy program uruchamiany jest bez ./program na poczatku) */
    int start_idx = 0;
    if(argc > 0 && argv[0][0] != '-'){
        start_idx = 1;  /* standardowe uruchomienie z nazwa programu */
    }
    
    for(int i = start_idx; i < argc; i++){
        /* sprawdzenie, czy argument jest poprawna opcja */
        if(argv[i][0] == '-'){
            if(strlen(argv[i]) < 2){
                fprintf(stderr, "Ostrzezenie: Znaleziono nieprawidlowy argument: %s\n", argv[i]);
                continue;
            }
            
            switch(argv[i][1]){
                case 'i':
                    if(i + 1 < argc){
                        *plik_wejsciowy = argv[++i];
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -i\n");
                    }
                    break;
                    
                case 'o':
                    if(i + 1 < argc){
                        *plik_wyjsciowy = argv[++i];
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -o\n");
                    }
                    break;
                    
                case 'p':
                    if(i + 1 < argc){
                        if(jest_liczba_calkowita(argv[i+1])){
                            int wartosc = atoi(argv[++i]);
                            if(wartosc > 0){
                                *liczba_czesci = wartosc;
                            }else{
                                fprintf(stderr, "Ostrzezenie: Liczba czesci musi byc dodatnia. Uzywam domyslnej wartosci: %d\n", *liczba_czesci);
                            }
                        }else{
                            fprintf(stderr, "Ostrzezenie: Oczekiwano liczby calkowitej po -p. Uzywam domyslnej wartosci: %d\n", *liczba_czesci);
                            i++;
                        }
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -p\n");
                    }
                    break;
                    
                case 'm':
                    if(i + 1 < argc){
                        if(jest_liczba_calkowita(argv[i+1])){
                            int wartosc = atoi(argv[++i]);
                            if(wartosc >= 0 && wartosc <= 100){
                                *margines_procentowy = wartosc;
                            }else{
                                fprintf(stderr, "Ostrzezenie: Margines procentowy musi byc z zakresu 0-100. Uzywam domyslnej wartosci: %d\n", *margines_procentowy);
                            }
                        }else{
                            fprintf(stderr, "Ostrzezenie: Oczekiwano liczby calkowitej po -m. Uzywam domyslnej wartosci: %d\n", *margines_procentowy);
                            i++;
                        }
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -m\n");
                    }
                    break;
                    
                case 'f':
                    if(i + 1 < argc){
                        if(strcmp(argv[i+1], "txt") == 0 || strcmp(argv[i+1], "bin") == 0){
                            *format = argv[++i];
                        }else{
                            fprintf(stderr, "Ostrzezenie: Format musi byc 'txt' lub 'bin'. Uzywam domyslnej wartosci: %s\n", *format);
                            i++;
                        }
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -f\n");
                    }
                    break;
                    
                case 'a':
                    if(i + 1 < argc){
                        if(strcmp(argv[i+1], "modulo") == 0 || 
                            strcmp(argv[i+1], "sekwencyjny") == 0 || 
                            strcmp(argv[i+1], "losowy") == 0){
                            *typ_algorytmu = argv[++i];
                        }else{
                            fprintf(stderr, "Ostrzezenie: Algorytm musi byc 'modulo', 'sekwencyjny' lub 'losowy'. Uzywam domyslnej wartosci: %s\n", *typ_algorytmu);
                            i++;
                        }
                    }else{
                        fprintf(stderr, "Blad: Brak wartosci dla opcji -a\n");
                    }
                    break;
                    
                case 'y':
                    *uzyj_hybrydowy = 1;
                    printf("Wlaczono algorytm hybrydowy.\n");
                    break;
                    
                case 'h':
                    wyswietl_uzycie(argv[0]);
                    exit(EXIT_SUCCESS);
                    break;
                    
                default:
                    fprintf(stderr, "Ostrzezenie: Nieznana opcja: -%c\n", argv[i][1]);
                    break;
            }
        }else{
            /* jesli argument nie zaczyna sie od -, a jest pierwszym, moze to byc nazwa pliku wejsciowego */
            if(!*plik_wejsciowy){
                *plik_wejsciowy = argv[i];
                printf("Znaleziono nienazwany argument, interpretuje jako plik wejsciowy: %s\n", argv[i]);
            }else if(!*plik_wyjsciowy){
                *plik_wyjsciowy = argv[i];
                printf("Znaleziono drugi nienazwany argument, interpretuje jako plik wyjsciowy: %s\n", argv[i]);
            }else{
                fprintf(stderr, "Ostrzezenie: Ignorowanie dodatkowego argumentu: %s\n", argv[i]);
            }
        }
    }

    /* wyswietl podsumowanie znalezionych parametrow */
    printf("Parametry:\n");
    printf("  Plik wejsciowy: %s\n", *plik_wejsciowy ? *plik_wejsciowy : "(nie podano)");
    printf("  Plik wyjsciowy: %s\n", *plik_wyjsciowy ? *plik_wyjsciowy : "(nie podano)");
    printf("  Liczba czesci: %d\n", *liczba_czesci);
    printf("  Margines procentowy: %d%%\n", *margines_procentowy);
    printf("  Format wyjscia: %s\n", *format);
    
    if(*uzyj_hybrydowy){
        printf("  Algorytm: hybrydowy\n");
    }else{
        printf("  Algorytm podzialu: %s\n", *typ_algorytmu);
    }
}