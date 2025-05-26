#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "graf.h"

// Pomocnicza struktura dla wczytywania danych CSRRG
typedef struct {
    int max_secondary_value;
    int *secondary_data;
    int secondary_data_count;
    int *secondary_row_ptr;
    int secondary_row_ptr_count;
    int *graph_neighbors;
    int graph_neighbors_count;
    int *graph_row_ptr;
    int graph_row_ptr_count;
} CSRRGData;

// Deklaracje funkcji pomocniczych
static void free_csrrg_data_partial(CSRRGData* data);
static char* read_line(FILE* file);
static int* parse_line(const char* line, int* count);

// Funkcja parsujaca linie tekstu na tablice liczb calkowitych
static int* parse_line(const char* line, int* count) {
    if (!line || !count) {
        *count = 0;
        return NULL;
    }
    
    // Najpierw policz liczbe liczb calkowitych
    const char* p = line;
    int num_ints = 0;
    char* endptr;
    
    while (*p) {
        // Pomin biale znaki i separatory
        while (*p && (*p == ';' || isspace(*p))) p++;
        if (!*p) break;
        
        // Sprobuj sparsowac liczbe
        strtol(p, &endptr, 10);
        if (p != endptr) {
            num_ints++;
            p = endptr;
        } else {
            // Nie jest liczba, pomin do nastepnego separatora
            while (*p && *p != ';') p++;
        }
    }
    
    if (num_ints == 0) {
        *count = 0;
        return NULL;
    }
    
    // Alokuj pamiec na liczby
    int* numbers = (int*)malloc(num_ints * sizeof(int));
    if (!numbers) {
        fprintf(stderr, "Blad alokacji pamieci (parse_line numbers)\n");
        *count = 0;
        return NULL;
    }
    
    // Faktyczne parsowanie liczb
    p = line;
    int i = 0;
    
    while (*p && i < num_ints) {
        // Pomin biale znaki i separatory
        while (*p && (*p == ';' || isspace(*p))) p++;
        if (!*p) break;
        
        // Parsuj liczbe
        long val = strtol(p, &endptr, 10);
        if (p != endptr) {
            numbers[i++] = (int)val;
            p = endptr;
        } else {
            // Nie jest liczba, pomin do nastepnego separatora
            while (*p && *p != ';') p++;
        }
    }
    
    *count = i;
    return numbers;
}

// Funkcja do wczytywania linii z pliku
static char* read_line(FILE* file) {
    if (!file) return NULL;
    
    size_t buffer_size = 1024; // Poczatkowy rozmiar bufora
    char* buffer = (char*)malloc(buffer_size);
    if (!buffer) {
        fprintf(stderr, "Blad alokacji pamieci (read_line buffer)\n");
        return NULL;
    }
    
    size_t pos = 0;
    int c;
    
    while ((c = fgetc(file)) != EOF && c != '\n') {
        // Sprawdz czy potrzebujemy zwiekszyc bufor
        if (pos >= buffer_size - 1) {
            buffer_size *= 2;
            char* new_buffer = (char*)realloc(buffer, buffer_size);
            if (!new_buffer) {
                fprintf(stderr, "Blad alokacji pamieci (read_line buffer resize)\n");
                free(buffer);
                return NULL;
            }
            buffer = new_buffer;
        }
        
        buffer[pos++] = (char)c;
    }
    
    // Jesli pierwszy odczytany znak to EOF, jestesmy na koncu pliku
    if (c == EOF && pos == 0) {
        free(buffer);
        return NULL;
    }
    
    // Zakoncz ciag znakiem null
    buffer[pos] = '\0';
    
    // Usun koncowy '\r' jesli istnieje (dla plikow Windows)
    if (pos > 0 && buffer[pos-1] == '\r') {
        buffer[pos-1] = '\0';
    }
    
    return buffer;
}

// Funkcja do zwalniania pamieci tymczasowej struktury CSRRG
static void free_csrrg_data_partial(CSRRGData* data) {
    if (data) {
        if (data->secondary_data) free(data->secondary_data);
        if (data->secondary_row_ptr) free(data->secondary_row_ptr);
        free(data);
    }
}

// Funkcja wczytujaca dane z pliku CSRRG
static CSRRGData* read_csrrg_data(const char* filename) {
    FILE* file = fopen(filename, "r");
    if (!file) {
        fprintf(stderr, "Blad otwierania pliku: %s\n", filename);
        return NULL;
    }
    
    CSRRGData* data = (CSRRGData*)malloc(sizeof(CSRRGData));
    if (!data) {
        fprintf(stderr, "Blad alokacji pamieci dla CSRRGData\n");
        fclose(file);
        return NULL;
    }
    
    // Inicjalizacja struktury
    data->max_secondary_value = 0;
    data->secondary_data = NULL;
    data->secondary_data_count = 0;
    data->secondary_row_ptr = NULL;
    data->secondary_row_ptr_count = 0;
    data->graph_neighbors = NULL;
    data->graph_neighbors_count = 0;
    data->graph_row_ptr = NULL;
    data->graph_row_ptr_count = 0;
    
    int line_number = 0;
    char* line;
    
    // Wczytywanie linii z pliku
    while (line_number < 5 && (line = read_line(file)) != NULL) {
        // Pominiecie pustych linii
        if (strlen(line) == 0) {
            free(line);
            continue;
        }
        
        line_number++;
        
        switch (line_number) {
            case 1: // Wartosc z Linii 1
                data->max_secondary_value = atoi(line);
                break;
                
            case 2: // Dane struktury dodatkowej (Linia 2)
                data->secondary_data = parse_line(line, &data->secondary_data_count);
                break;
                
            case 3: // Wskazniki struktury dodatkowej (Linia 3)
                data->secondary_row_ptr = parse_line(line, &data->secondary_row_ptr_count);
                break;
                
            case 4: // Sasiedzi grafu glownego (Linia 4)
                data->graph_neighbors = parse_line(line, &data->graph_neighbors_count);
                if (!data->graph_neighbors && data->graph_neighbors_count > 0) {
                    fprintf(stderr, "Blad parsowania sasiadow grafu (Linia 4)\n");
                    free(line); 
                    fclose(file); 
                    free_csrrg_data_partial(data); 
                    return NULL;
                }
                break;
                
            case 5: // Wskazniki grafu glownego (Linia 5)
                data->graph_row_ptr = parse_line(line, &data->graph_row_ptr_count);
                if (!data->graph_row_ptr && data->graph_row_ptr_count > 0) {
                    fprintf(stderr, "Blad parsowania wskaznikow grafu (Linia 5)\n");
                    free(line); 
                    fclose(file); 
                    free_csrrg_data_partial(data); 
                    if (data->graph_neighbors) free(data->graph_neighbors);
                    return NULL;
                }
                break;
        }
        
        free(line);
    }
    
    fclose(file);
    
    // Sprawdzenie, czy wszystkie wymagane dane dla grafu glownego zostaly wczytane
    if (!data->graph_neighbors || !data->graph_row_ptr) {
        fprintf(stderr, "Niekompletne lub nieprawidlowe dane grafu\n");
        if (data->graph_neighbors) free(data->graph_neighbors);
        if (data->graph_row_ptr) free(data->graph_row_ptr);
        free_csrrg_data_partial(data);
        return NULL;
    }

    // Podstawowa weryfikacja wskaznikow grafu glownego
    if (data->graph_row_ptr_count < 2) {
        fprintf(stderr, "Nieprawidlowe wskazniki grafu (Linia 5 count < 2)\n");
        free(data->graph_neighbors);
        free(data->graph_row_ptr);
        free_csrrg_data_partial(data);
        return NULL;
    }
    
    return data;
}

// Glowna funkcja wczytujaca graf z pliku
Graf* wczytaj_graf(const char* nazwa_pliku) {
    if (!nazwa_pliku) {
        fprintf(stderr, "Nieprawidlowa nazwa pliku (NULL)\n");
        return NULL;
    }
    
    CSRRGData* csrrg_data = read_csrrg_data(nazwa_pliku);
    if (!csrrg_data) {
        return NULL;
    }
    
    Graf* graf = (Graf*)malloc(sizeof(Graf));
    if (!graf) {
        fprintf(stderr, "Blad alokacji pamieci dla struktury Graf\n");
        if (csrrg_data->secondary_data) free(csrrg_data->secondary_data);
        if (csrrg_data->secondary_row_ptr) free(csrrg_data->secondary_row_ptr);
        if (csrrg_data->graph_neighbors) free(csrrg_data->graph_neighbors);
        if (csrrg_data->graph_row_ptr) free(csrrg_data->graph_row_ptr);
        free(csrrg_data);
        return NULL;
    }
    
    // Liczba wierzcholkow to liczba elementow w graph_row_ptr minus 1
    graf->liczba_wierzcholkow = csrrg_data->graph_row_ptr_count - 1;
    
    // Sprawdz podstawowa poprawnosc
    if (graf->liczba_wierzcholkow <= 0) {
        fprintf(stderr, "Nieprawidlowa liczba wierzcholkow (%d)\n", graf->liczba_wierzcholkow);
        if (csrrg_data->secondary_data) free(csrrg_data->secondary_data);
        if (csrrg_data->secondary_row_ptr) free(csrrg_data->secondary_row_ptr);
        if (csrrg_data->graph_neighbors) free(csrrg_data->graph_neighbors);
        if (csrrg_data->graph_row_ptr) free(csrrg_data->graph_row_ptr);
        free(csrrg_data);
        free(graf);
        return NULL;
    }

    // Wskazniki listy sasiedztwa to graph_row_ptr
    graf->wskazniki_listy = csrrg_data->graph_row_ptr;
    
    // Splaszczona lista sasiadow to graph_neighbors
    graf->lista_sasiedztwa = csrrg_data->graph_neighbors;
    
    // Calkowita liczba elementow w liscie sasiadow
    int total_adj_list_entries = csrrg_data->graph_neighbors_count;
    
    // Obliczanie liczby krawedzi (dla grafu nieskierowanego, kazda krawedz jest liczona dwukrotnie)
    graf->liczba_krawedzi = total_adj_list_entries / 2;
    
    // Zwalnianie struktury tymczasowej (NIE zwalniamy graph_neighbors i graph_row_ptr, ktore teraz naleza do graf)
    csrrg_data->graph_neighbors = NULL;
    csrrg_data->graph_row_ptr = NULL;
    free_csrrg_data_partial(csrrg_data);
    
    return graf;
}

// Funkcja do zwalniania pamieci grafu
void zwolnij_graf(Graf* graf) {
    if (graf) {
        if (graf->lista_sasiedztwa) free(graf->lista_sasiedztwa);
        if (graf->wskazniki_listy) free(graf->wskazniki_listy);
        free(graf);
    }
}