package com.example.graphpartitioner.model;

import java.util.Arrays;

/**
 * Reprezentacja podziału grafu na części
 */
public class Partition {
    private final int[] assignments;     // Mapuje ID wierzchołka na ID części
    private final int[] partSizes;       // Rozmiar każdej części
    private final int partCount;         // Liczba części
    private int cutEdges;                // Liczba przeciętych krawędzi
    private final int marginPercent;     // Maksymalny dozwolony margines procentowy
    
    /**
     * Konstruktor dla nowego, pustego podziału
     */
    public Partition(int partCount, int numVertices, int marginPercent) {
        this.partCount = partCount;
        this.marginPercent = marginPercent;
        this.assignments = new int[numVertices];
        this.partSizes = new int[partCount];
        this.cutEdges = 0;
        
        // Inicjalizacja - wszystkie wierzchołki nieprzypisane (-1)
        Arrays.fill(assignments, -1);
    }
    
    /**
     * Konstruktor z pełnymi danymi
     */
    public Partition(int[] assignments, int[] partSizes, int partCount, int cutEdges, int marginPercent) {
        this.assignments = assignments;
        this.partSizes = partSizes;
        this.partCount = partCount;
        this.cutEdges = cutEdges;
        this.marginPercent = marginPercent;
    }
    
    /**
     * Zwraca przypisanie wierzchołka do części
     */
    public int getAssignment(int vertex) {
        if (vertex < 0 || vertex >= assignments.length) {
            return -1;
        }
        return assignments[vertex];
    }
    
    /**
     * Ustawia przypisanie wierzchołka do części
     */
    public void setAssignment(int vertex, int partId) {
        if (vertex < 0 || vertex >= assignments.length || partId < 0 || partId >= partCount) {
            throw new IllegalArgumentException("Invalid vertex or part ID");
        }
        
        int oldPartId = assignments[vertex];
        
        // Aktualizuj rozmiary części
        if (oldPartId >= 0 && oldPartId < partCount) {
            partSizes[oldPartId]--;
        }
        partSizes[partId]++;
        
        assignments[vertex] = partId;
    }
    
    /**
     * Tworzy głęboką kopię podziału
     */
    public Partition copy(int numVertices) {
        int[] newAssignments = Arrays.copyOf(assignments, numVertices);
        int[] newPartSizes = Arrays.copyOf(partSizes, partCount);
        return new Partition(newAssignments, newPartSizes, partCount, cutEdges, marginPercent);
    }
    
    /**
     * Oblicza średni rozmiar części
     */
    public int getAveragePartSize() {
        return assignments.length / partCount;
    }
    
    /**
     * Oblicza maksymalną dozwoloną nierówność wielkości części
     */
    public int getMaxImbalance() {
        int avgSize = getAveragePartSize();
        int maxImbalance = (avgSize * marginPercent) / 100;
        return Math.max(maxImbalance, 1);
    }
    
    // Gettery
    public int getPartCount() {
        return partCount;
    }
    
    public int[] getAssignments() {
        return assignments;
    }
    
    public int[] getPartSizes() {
        return partSizes;
    }
    
    public int getCutEdges() {
        return cutEdges;
    }
    
    public void setCutEdges(int cutEdges) {
        this.cutEdges = cutEdges;
    }
    
    public int getMarginPercent() {
        return marginPercent;
    }
    
    public int getVertexCount() {
        return assignments.length;
    }
    
    /**
     * Sprawdza czy podział jest zbalansowany
     */
    public boolean isBalanced() {
        int avgSize = getAveragePartSize();
        int maxImbalance = getMaxImbalance();
        
        for (int size : partSizes) {
            if (Math.abs(size - avgSize) > maxImbalance) {
                return false;
            }
        }
        return true;
    }
}