package com.example.graphpartitioner.algorithms;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

import java.util.*;

/**
 * Implementacja algorytmu Kernighana-Lina do optymalizacji podziału grafu
 */
public class KernighanLin {
    
    /**
     * Struktura reprezentująca ruch w algorytmie
     */
    private static class Move {
        int vertex;
        int oldPart;
        int newPart;
        int gain;
        
        Move(int vertex, int oldPart, int newPart, int gain) {
            this.vertex = vertex;
            this.oldPart = oldPart;
            this.newPart = newPart;
            this.gain = gain;
        }
    }
    
    /**
     * Oblicza zysk z przeniesienia wierzchołka do innej części
     * Zysk = (liczba sąsiadów w nowej części) - (liczba sąsiadów w obecnej części)
     */
    private static int calculateGain(Graph graph, Partition partition, int vertex, int newPart) {
        int currentPart = partition.getAssignment(vertex);
        
        // Liczba sąsiadów w obecnej części
        int neighborsInCurrent = PartitionUtils.countNeighborsInPart(graph, partition, vertex, currentPart);
        
        // Liczba sąsiadów w nowej części
        int neighborsInNew = PartitionUtils.countNeighborsInPart(graph, partition, vertex, newPart);
        
        // Zysk
        return neighborsInNew - neighborsInCurrent;
    }
    
    /**
     * Wykonuje jedno przejście algorytmu Kernighana-Lina
     * Zwraca true jeśli dokonano poprawy
     */
    private static boolean kernighanLinPass(Graph graph, Partition partition) {
        int numVertices = graph.getVertexCount();
        int numParts = partition.getPartCount();
        
        if (numVertices == 0 || numParts <= 1) {
            return false;
        }
        
        // Struktury tymczasowe
        List<Move> moves = new ArrayList<>();
        int[] cumulativeGain = new int[numVertices + 1];
        boolean[] moved = new boolean[numVertices];
        
        // Zapisz oryginalny stan podziału
        Partition originalPartition = partition.copy(numVertices);
        
        // Inicjalizacja
        cumulativeGain[0] = 0;
        
        // Faza 1: Znajdowanie sekwencji ruchów
        for (int step = 0; step < numVertices; step++) {
            int bestGain = -1; // Szukamy zysku > 0
            int bestVertex = -1;
            int bestTargetPart = -1;
            
            // Sprawdź każdy wierzchołek
            for (int v = 0; v < numVertices; v++) {
                if (moved[v]) continue; // Pomiń już przeniesione wierzchołki
                
                int currentPart = partition.getAssignment(v);
                
                // Sprawdź każdą możliwą docelową część
                for (int targetPart = 0; targetPart < numParts; targetPart++) {
                    if (targetPart == currentPart) continue;
                    
                    // Sprawdź ograniczenia rozmiaru części
                    if (!PartitionUtils.canMoveVertex(partition, v, targetPart)) {
                        continue;
                    }
                    
                    // Oblicz zysk z przeniesienia
                    int gain = calculateGain(graph, partition, v, targetPart);
                    
                    // Aktualizuj najlepszy ruch
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestVertex = v;
                        bestTargetPart = targetPart;
                    }
                }
            }
            
            // Jeśli nie znaleziono korzystnego ruchu, zakończ
            if (bestVertex == -1 || bestGain <= 0) {
                break;
            }
            
            // Zapisz ruch
            Move move = new Move(bestVertex, partition.getAssignment(bestVertex), 
                                bestTargetPart, bestGain);
            moves.add(move);
            
            // Aktualizuj kumulatywny zysk
            cumulativeGain[moves.size()] = cumulativeGain[moves.size() - 1] + bestGain;
            
            // Tymczasowo przenieś wierzchołek
            partition.setAssignment(bestVertex, bestTargetPart);
            
            // Oznacz jako przeniesiony
            moved[bestVertex] = true;
        }
        
        // Faza 2: Znajdź prefiks z maksymalnym zyskiem
        int maxCumulativeGain = 0;
        int bestPrefixLength = 0;
        
        for (int i = 1; i <= moves.size(); i++) {
            if (cumulativeGain[i] > maxCumulativeGain) {
                maxCumulativeGain = cumulativeGain[i];
                bestPrefixLength = i;
            }
        }
        
        // Faza 3: Przywróć oryginalny stan i zastosuj tylko najlepszy prefiks ruchów
        // Kopiujemy dane z oryginalnego podziału
        for (int v = 0; v < numVertices; v++) {
            partition.setAssignment(v, originalPartition.getAssignment(v));
        }
        
        // Zastosuj tylko te ruchy, które tworzą najlepszy prefiks
        if (maxCumulativeGain > 0) {
            for (int i = 0; i < bestPrefixLength; i++) {
                Move move = moves.get(i);
                partition.setAssignment(move.vertex, move.newPart);
            }
            
            // Zaktualizuj liczbę przeciętych krawędzi
            int newCutEdges = partition.getCutEdges() - maxCumulativeGain;
            partition.setCutEdges(newCutEdges);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Główna funkcja algorytmu Kernighana-Lina
     * Optymalizuje podział grafu poprzez iteracyjne przejścia
     */
    public static void optimizeWithKernighanLin(Graph graph, Partition partition, int maxIterations) {
        if (graph == null || partition == null || 
            graph.getVertexCount() == 0 || partition.getPartCount() <= 1) {
            return;
        }
        
        // Ustaw domyślną liczbę iteracji jeśli nie podano
        if (maxIterations <= 0) {
            maxIterations = 50;
            if (graph.getVertexCount() > 5000) {
                maxIterations = 20;
                System.out.println("Duży graf wykryty (" + graph.getVertexCount() + 
                                 " wierzchołków) - ograniczenie do " + maxIterations + " iteracji KL");
            }
        }
        
        System.out.println("Początkowa liczba przeciętych krawędzi: " + partition.getCutEdges());
        
        // Powtarzaj przejścia KL, aż nie będzie więcej poprawy
        int iteration = 0;
        boolean improvement = true;
        
        while (improvement && iteration < maxIterations) {
            improvement = kernighanLinPass(graph, partition);
            iteration++;
            
            if (improvement) {
                System.out.println("Iteracja " + iteration + 
                                 ": Przecięte krawędzie = " + partition.getCutEdges());
            } else {
                System.out.println("Brak poprawy w iteracji " + iteration + 
                                 ", koniec algorytmu.");
            }
        }
        
        // Zakończenie algorytmu
        System.out.println("Wyjście z algorytmu KL po " + iteration + " iteracjach.");
        System.out.println("Końcowa liczba przeciętych krawędzi: " + partition.getCutEdges());
        
        // Ostateczna weryfikacja liczby przeciętych krawędzi
        int finalEdges = PartitionUtils.calculateCutEdges(graph, partition);
        if (finalEdges != partition.getCutEdges()) {
            System.out.println("Korekta końcowej liczby przeciętych krawędzi: zmiana z " + 
                             partition.getCutEdges() + " na " + finalEdges);
            partition.setCutEdges(finalEdges);
        }
    }
    
    /**
     * Wersja algorytmu bez wypisywania komunikatów
     */
    public static void optimizeWithKernighanLinSilent(Graph graph, Partition partition, int maxIterations) {
        if (graph == null || partition == null || 
            graph.getVertexCount() == 0 || partition.getPartCount() <= 1) {
            return;
        }
        
        if (maxIterations <= 0) {
            maxIterations = graph.getVertexCount() > 5000 ? 20 : 50;
        }
        
        int iteration = 0;
        boolean improvement = true;
        
        while (improvement && iteration < maxIterations) {
            improvement = kernighanLinPass(graph, partition);
            iteration++;
        }
        
        // Weryfikacja końcowa
        int finalEdges = PartitionUtils.calculateCutEdges(graph, partition);
        partition.setCutEdges(finalEdges);
    }
}