package com.example.graphpartitioner.algorithms;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

import java.util.*;

/**
 * Klasa z narzędziami do obsługi podziałów grafu
 */
public class PartitionUtils {
    
    /**
     * Oblicza liczbę przeciętych krawędzi w podziale
     * Analogicznie do oblicz_przeciete_krawedzie z C
     */
    public static int calculateCutEdges(Graph graph, Partition partition) {
        if (graph == null || partition == null || graph.getVertexCount() == 0) {
            return 0;
        }
        
        int cutEdges = 0;
        
        // Iterujemy po wszystkich wierzchołkach
        for (int u = 0; u < graph.getVertexCount(); u++) {
            int partU = partition.getAssignment(u);
            
            // Sprawdzamy sąsiadów wierzchołka u
            List<Integer> neighbors = graph.getNeighbors(u);
            for (int v : neighbors) {
                // Liczymy krawędź tylko gdy u < v aby uniknąć podwójnego liczenia
                if (u < v && v < graph.getVertexCount()) {
                    int partV = partition.getAssignment(v);
                    
                    // Jeśli wierzchołki są w różnych częściach, krawędź jest przecięta
                    if (partU != partV) {
                        cutEdges++;
                    }
                }
            }
        }
        
        return cutEdges;
    }
    
    /**
     * Balansuje losowy podział
     * Analogicznie do zbalansuj_losowy_podzial z C
     */
    public static void balanceRandomPartition(Graph graph, Partition partition) {
        if (graph == null || partition == null || 
            graph.getVertexCount() == 0 || partition.getPartCount() <= 1) {
            return;
        }
        
        Random random = new Random();
        int avgSize = partition.getAveragePartSize();
        int maxImbalance = partition.getMaxImbalance();
        
        int maxIterations = graph.getVertexCount() / 2; // Limit bezpieczeństwa
        int iterations = 0;
        
        while (iterations < maxIterations) {
            // Znajdź części zbyt duże i zbyt małe
            List<Integer> partsAbove = new ArrayList<>();
            List<Integer> partsBelow = new ArrayList<>();
            
            for (int p = 0; p < partition.getPartCount(); p++) {
                int currentSize = partition.getPartSizes()[p];
                
                if (currentSize > avgSize + maxImbalance) {
                    partsAbove.add(p);
                } else if (currentSize < avgSize - maxImbalance) {
                    partsBelow.add(p);
                }
            }
            
            // Jeśli wszystko jest zbalansowane, kończymy
            if (partsAbove.isEmpty() || partsBelow.isEmpty()) {
                break;
            }
            
            // Przenieś wierzchołki z części zbyt dużych do zbyt małych
            int transfers = Math.min(partsAbove.size(), partsBelow.size());
            for (int i = 0; i < transfers; i++) {
                int sourcePart = partsAbove.get(i);
                int targetPart = partsBelow.get(i);
                
                // Znajdź wierzchołki w części źródłowej
                List<Integer> candidates = new ArrayList<>();
                for (int v = 0; v < graph.getVertexCount(); v++) {
                    if (partition.getAssignment(v) == sourcePart) {
                        candidates.add(v);
                    }
                }
                
                if (!candidates.isEmpty()) {
                    // Wybierz losowy wierzchołek
                    int v = candidates.get(random.nextInt(candidates.size()));
                    
                    // Przenieś wierzchołek
                    partition.setAssignment(v, targetPart);
                }
            }
            
            iterations++;
        }
        
        // Zaktualizuj liczbę przeciętych krawędzi
        partition.setCutEdges(calculateCutEdges(graph, partition));
    }
    
    /**
     * Oblicza liczbę sąsiadów wierzchołka w danej części
     */
    public static int countNeighborsInPart(Graph graph, Partition partition, int vertex, int partId) {
        int count = 0;
        List<Integer> neighbors = graph.getNeighbors(vertex);
        
        for (int neighbor : neighbors) {
            if (neighbor >= 0 && neighbor < graph.getVertexCount() && 
                partition.getAssignment(neighbor) == partId) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Sprawdza czy przeniesienie wierzchołka nie narusza ograniczeń równowagi
     */
    public static boolean canMoveVertex(Partition partition, int vertex, int targetPart) {
        int currentPart = partition.getAssignment(vertex);
        if (currentPart == targetPart) {
            return false;
        }
        
        int avgSize = partition.getAveragePartSize();
        int maxImbalance = partition.getMaxImbalance();
        
        // Sprawdź czy część docelowa nie będzie zbyt duża
        if (partition.getPartSizes()[targetPart] >= avgSize + maxImbalance) {
            return false;
        }
        
        // Sprawdź czy część źródłowa nie będzie zbyt mała
        if (partition.getPartSizes()[currentPart] <= avgSize - maxImbalance) {
            return false;
        }
        
        return true;
    }
}