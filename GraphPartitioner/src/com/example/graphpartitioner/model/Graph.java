package com.example.graphpartitioner.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Reprezentacja grafu w formacie CSR (Compressed Sparse Row)
 */
public class Graph {
    private final int vertexCount;
    private final int edgeCount;
    private final int[] rowPointers;
    private final int[] adjacencyList;
    
    public Graph(int vertexCount, int edgeCount, int[] rowPointers, int[] adjacencyList) {
        this.vertexCount = vertexCount;
        this.edgeCount = edgeCount;
        this.rowPointers = rowPointers;
        this.adjacencyList = adjacencyList;
    }
    
    /**
     * Zwraca listę sąsiadów danego wierzchołka
     */
    public List<Integer> getNeighbors(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            return new ArrayList<>();
        }
        
        List<Integer> neighbors = new ArrayList<>();
        int start = rowPointers[vertex];
        int end = rowPointers[vertex + 1];
        
        for (int i = start; i < end; i++) {
            neighbors.add(adjacencyList[i]);
        }
        
        return neighbors;
    }
    
    /**
     * Sprawdza czy istnieje krawędź między wierzchołkami u i v
     */
    public boolean hasEdge(int u, int v) {
        if (u < 0 || u >= vertexCount || v < 0 || v >= vertexCount) {
            return false;
        }
        
        int start = rowPointers[u];
        int end = rowPointers[u + 1];
        
        for (int i = start; i < end; i++) {
            if (adjacencyList[i] == v) {
                return true;
            }
        }
        
        return false;
    }
    
    // Gettery
    public int getVertexCount() {
        return vertexCount;
    }
    
    public int getEdgeCount() {
        return edgeCount;
    }
    
    public int[] getRowPointers() {
        return rowPointers;
    }
    
    public int[] getAdjacencyList() {
        return adjacencyList;
    }
    
    /**
     * Oblicza gęstość grafu
     */
    public double getDensity() {
        if (vertexCount <= 1) {
            return 0.0;
        }
        double maxEdges = (double) vertexCount * (vertexCount - 1) / 2.0;
        return edgeCount / maxEdges;
    }
}