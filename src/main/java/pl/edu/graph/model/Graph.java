package pl.edu.graph.model;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    private int vertexCount;
    private int edgeCount;
    private int[] rowPointers;
    private int[] adjacencyList;
    
    public Graph(int vertexCount, int edgeCount, int[] rowPointers, int[] adjacencyList) {
        this.vertexCount = vertexCount;
        this.edgeCount = edgeCount;
        this.rowPointers = rowPointers;
        this.adjacencyList = adjacencyList;
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public int getEdgeCount() {
        return edgeCount;
    }
    
    public List<Integer> getNeighbors(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException("Invalid vertex index");
        }
        
        List<Integer> neighbors = new ArrayList<>();
        
        int start = rowPointers[vertex];
        int end = rowPointers[vertex + 1];
        
        for (int i = start; i < end; i++) {
            neighbors.add(adjacencyList[i]);
        }
        
        return neighbors;
    }
}