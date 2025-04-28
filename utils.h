#ifndef UTILS_H
#define UTILS_H

/* funkcje pomocnicze */
void wyswietl_uzycie(const char* nazwa_programu);
void parsuj_argumenty(int argc, char* argv[], char** plik_wejsciowy, char** plik_wyjsciowy, 
                     int* liczba_czesci, int* margines_procentowy, char** format, 
                     char** typ_algorytmu, int* uzyj_hybrydowy);

#endif // UTILS_H