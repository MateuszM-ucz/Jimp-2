package com.example.graphpartitioner.io;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

import java.io.*;
import java.util.*;

/**
 * Klasa do zapisywania grafów i podziałów do różnych formatów plików
 */
public class GraphSaver {
    
    /**
     * Format 1: Tekstowy (macierz + przypisania)
     * Analogicznie do zapisz_podzial_tekstowy z C
     */
    public static void savePartitionToAdjacencyMatrixText(String filePath, Graph graph, Partition partition) 
            throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Nagłówek z informacjami o podziale
            writer.println("# Podzial grafu na " + partition.getPartCount() + " czesci");
            writer.println("# Liczba wierzcholkow: " + graph.getVertexCount());
            writer.println("# Liczba przecietych krawedzi: " + partition.getCutEdges());
            writer.println();
            
            // Macierz sąsiedztwa
            writer.println("# Macierz sasiedztwa:");
            
            int n = graph.getVertexCount();
            for (int i = 0; i < n; i++) {
                writer.print("[");
                
                // Stwórz wiersz macierzy
                boolean[] row = new boolean[n];
                List<Integer> neighbors = graph.getNeighbors(i);
                for (int neighbor : neighbors) {
                    if (neighbor >= 0 && neighbor < n) {
                        row[neighbor] = true;
                    }
                }
                
                // Zapisz wiersz
                for (int j = 0; j < n; j++) {
                    writer.print(row[j] ? "1." : "0.");
                    if (j < n - 1) {
                        writer.print(" ");
                    }
                }
                writer.println("]");
            }
            
            // Lista przypisań
            writer.println();
            writer.println("# Lista przypisan wierzcholkow do czesci:");
            writer.println("# Format: <id_wierzcholka> - <id_czesci>");
            
            for (int v = 0; v < n; v++) {
                writer.println(v + " - " + partition.getAssignment(v));
            }
        }
    }
    
    /**
     * Format 2: "Binarny" (CSRRG tekstowy)
     * Analogicznie do zapisz_podzial_binarny z C
     */
    public static void savePartitionToCsrrgText(String filePath, Graph graph, Partition partition, 
                                                CsrrgDataHolder initialCsrrgData) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Linia 1: liczba części w podziale
            writer.println(partition.getPartCount());
            
            // Linia 2: wierzchołki przypisane do poszczególnych części
            List<List<Integer>> verticesByPart = new ArrayList<>();
            for (int i = 0; i < partition.getPartCount(); i++) {
                verticesByPart.add(new ArrayList<>());
            }
            
            for (int v = 0; v < graph.getVertexCount(); v++) {
                int partId = partition.getAssignment(v);
                if (partId >= 0 && partId < partition.getPartCount()) {
                    verticesByPart.get(partId).add(v);
                }
            }
            
            // Zapisz płaską listę wierzchołków
            boolean first = true;
            for (List<Integer> vertices : verticesByPart) {
                for (int v : vertices) {
                    if (!first) {
                        writer.print(";");
                    }
                    writer.print(v);
                    first = false;
                }
            }
            writer.println();
            
            // Linia 3: wskaźniki wierszy dla struktury części
            writer.print("0");
            int pointer = 0;
            for (List<Integer> vertices : verticesByPart) {
                pointer += vertices.size();
                writer.print(";" + pointer);
            }
            writer.println();
            
            // Linia 4: lista sąsiedztwa grafu głównego
            int[] adjacencyList = graph.getAdjacencyList();
            if (adjacencyList.length > 0) {
                writer.print(adjacencyList[0]);
                for (int i = 1; i < adjacencyList.length; i++) {
                    writer.print(";" + adjacencyList[i]);
                }
            }
            writer.println();
            
            // Linia 5: wskaźniki wierszy grafu głównego
            int[] rowPointers = graph.getRowPointers();
            writer.print(rowPointers[0]);
            for (int i = 1; i < rowPointers.length; i++) {
                writer.print(";" + rowPointers[i]);
            }
            writer.println();
            
            // Linie 6+: wskaźniki i listy sąsiedztwa dla grafów części
            for (int p = 0; p < partition.getPartCount(); p++) {
                List<Integer> partVertices = verticesByPart.get(p);
                int partSize = partVertices.size();
                
                // Mapa z globalnych indeksów na lokalne
                Map<Integer, Integer> globalToLocal = new HashMap<>();
                for (int i = 0; i < partSize; i++) {
                    globalToLocal.put(partVertices.get(i), i);
                }
                
                // Buduj listę sąsiedztwa dla podgrafu
                List<Integer> subgraphAdjList = new ArrayList<>();
                int[] subgraphRowPtrs = new int[partSize + 1];
                
                for (int localIdx = 0; localIdx < partSize; localIdx++) {
                    int globalIdx = partVertices.get(localIdx);
                    subgraphRowPtrs[localIdx] = subgraphAdjList.size();
                    
                    List<Integer> neighbors = graph.getNeighbors(globalIdx);
                    for (int neighbor : neighbors) {
                        if (partition.getAssignment(neighbor) == p && globalToLocal.containsKey(neighbor)) {
                            subgraphAdjList.add(globalToLocal.get(neighbor));
                        }
                    }
                }
                subgraphRowPtrs[partSize] = subgraphAdjList.size();
                
                // Zapisz wskaźniki wierszy dla podgrafu
                if (partSize > 0) {
                    writer.print(subgraphRowPtrs[0]);
                    for (int i = 1; i <= partSize; i++) {
                        writer.print(";" + subgraphRowPtrs[i]);
                    }
                } else {
                    writer.print("0");
                }
                writer.println();
                
                // Zapisz listę sąsiedztwa dla podgrafu
                if (!subgraphAdjList.isEmpty()) {
                    writer.print(subgraphAdjList.get(0));
                    for (int i = 1; i < subgraphAdjList.size(); i++) {
                        writer.print(";" + subgraphAdjList.get(i));
                    }
                }
                writer.println();
            }
        }
    }
}