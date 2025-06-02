package com.example.graphpartitioner.io;

/**
 * Pomocnicza klasa do przechowywania danych z pliku CSRRG
 */
public class CsrrgDataHolder {
    private final int maxSecondaryValue;
    private final int[] secondaryData;
    private final int[] secondaryRowPtr;
    private final int[] graphNeighbors;
    private final int[] graphRowPtr;
    
    public CsrrgDataHolder(int maxSecondaryValue, int[] secondaryData, 
                          int[] secondaryRowPtr, int[] graphNeighbors, int[] graphRowPtr) {
        this.maxSecondaryValue = maxSecondaryValue;
        this.secondaryData = secondaryData;
        this.secondaryRowPtr = secondaryRowPtr;
        this.graphNeighbors = graphNeighbors;
        this.graphRowPtr = graphRowPtr;
    }
    
    // Gettery
    public int getMaxSecondaryValue() {
        return maxSecondaryValue;
    }
    
    public int[] getSecondaryData() {
        return secondaryData;
    }
    
    public int[] getSecondaryRowPtr() {
        return secondaryRowPtr;
    }
    
    public int[] getGraphNeighbors() {
        return graphNeighbors;
    }
    
    public int[] getGraphRowPtr() {
        return graphRowPtr;
    }
}