package com.example.graphpartitioner.algorithms;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

import java.util.Random;

/**
 * Klasa z metodami inicjalizacji podziałów grafu
 */
public class PartitionInitializer {
    
    /**
     * Inicjalizacja metodą modulo
     * Wierzchołek i idzie do części i % liczba_części
     */
    public static Partition initializeModulo(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0) {
            throw new IllegalArgumentException("Invalid parameters for modulo initialization");
        }
        
        Partition partition = new Partition(numParts, graph.getVertexCount(), marginPercent);
        
        // Przypisz wierzchołki metodą modulo
        for (int i = 0; i < graph.getVertexCount(); i++) {
            int partId = i % numParts;
            partition.setAssignment(i, partId);
        }
        
        // Oblicz początkową liczbę przeciętych krawędzi
        partition.setCutEdges(PartitionUtils.calculateCutEdges(graph, partition));
        
        return partition;
    }
    
    /**
     * Inicjalizacja metodą sekwencyjną
     * Pierwsze n/k wierzchołków do części 0, następne n/k do części 1, itd.
     */
    public static Partition initializeSequential(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0) {
            throw new IllegalArgumentException("Invalid parameters for sequential initialization");
        }
        
        Partition partition = new Partition(numParts, graph.getVertexCount(), marginPercent);
        
        int verticesPerPart = graph.getVertexCount() / numParts;
        int remainder = graph.getVertexCount() % numParts;
        
        int vertexIndex = 0;
        for (int p = 0; p < numParts; p++) {
            int partSize = verticesPerPart + (p < remainder ? 1 : 0);
            
            for (int i = 0; i < partSize && vertexIndex < graph.getVertexCount(); i++) {
                partition.setAssignment(vertexIndex, p);
                vertexIndex++;
            }
        }
        
        // Oblicz początkową liczbę przeciętych krawędzi
        partition.setCutEdges(PartitionUtils.calculateCutEdges(graph, partition));
        
        return partition;
    }
    
    /**
     * Inicjalizacja losowa
     * Losowo przypisuje wierzchołki do części, a następnie balansuje podział
     */
    public static Partition initializeRandom(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0) {
            throw new IllegalArgumentException("Invalid parameters for random initialization");
        }
        
        Partition partition = new Partition(numParts, graph.getVertexCount(), marginPercent);
        Random random = new Random();
        
        // Losowo przypisz każdy wierzchołek do części
        for (int i = 0; i < graph.getVertexCount(); i++) {
            int partId = random.nextInt(numParts);
            partition.setAssignment(i, partId);
        }
        
        // Balansuj podział
        PartitionUtils.balanceRandomPartition(graph, partition);
        
        return partition;
    }
    
    /**
     * Inicjalizacja DFS - alternatywna metoda
     * Używa przeszukiwania w głąb do grupowania połączonych wierzchołków
     */
    public static Partition initializeDFS(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0) {
            throw new IllegalArgumentException("Invalid parameters for DFS initialization");
        }
        
        Partition partition = new Partition(numParts, graph.getVertexCount(), marginPercent);
        boolean[] visited = new boolean[graph.getVertexCount()];
        int targetSizePerPart = graph.getVertexCount() / numParts;
        
        int currentPart = 0;
        int currentPartSize = 0;
        
        for (int start = 0; start < graph.getVertexCount() && currentPart < numParts; start++) {
            if (!visited[start]) {
                // DFS z ograniczeniem rozmiaru części
                java.util.Stack<Integer> stack = new java.util.Stack<>();
                stack.push(start);
                
                while (!stack.isEmpty() && currentPartSize < targetSizePerPart) {
                    int v = stack.pop();
                    
                    if (!visited[v]) {
                        visited[v] = true;
                        partition.setAssignment(v, currentPart);
                        currentPartSize++;
                        
                        // Dodaj sąsiadów do stosu
                        for (int neighbor : graph.getNeighbors(v)) {
                            if (!visited[neighbor]) {
                                stack.push(neighbor);
                            }
                        }
                    }
                }
                
                // Przejdź do następnej części jeśli obecna jest pełna
                if (currentPartSize >= targetSizePerPart && currentPart < numParts - 1) {
                    currentPart++;
                    currentPartSize = 0;
                }
            }
        }
        
        // Przypisz pozostałe nieodwiedzone wierzchołki
        for (int v = 0; v < graph.getVertexCount(); v++) {
            if (!visited[v]) {
                partition.setAssignment(v, currentPart);
            }
        }
        
        // Oblicz początkową liczbę przeciętych krawędzi
        partition.setCutEdges(PartitionUtils.calculateCutEdges(graph, partition));
        
        return partition;
    }
}