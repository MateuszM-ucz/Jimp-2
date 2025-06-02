package com.example.graphpartitioner.io;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;
import com.example.graphpartitioner.utils.Pair;

import java.io.*;
import java.util.*;

/**
 * Klasa do wczytywania grafów z różnych formatów plików
 */
public class GraphLoader {
    
    /**
     * Pomocnicza metoda do parsowania listy liczb całkowitych z linii tekstu
     */
    private static int[] parseIntegerList(String line, String delimiter) {
        if (line == null || line.trim().isEmpty()) {
            return new int[0];
        }
        
        String[] parts = line.split(delimiter);
        List<Integer> numbers = new ArrayList<>();
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    numbers.add(Integer.parseInt(trimmed));
                } catch (NumberFormatException e) {
                    // Ignoruj nieprawidłowe liczby
                }
            }
        }
        
        return numbers.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Format 1: Tekstowy CSR (z macierzy sąsiedztwa)
     * Pierwsze N linii to macierz sąsiedztwa, następnie opcjonalne przypisania
     */
    public static Pair<Graph, Partition> loadGraphFromAdjacencyMatrixText(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        List<String> assignmentLines = new ArrayList<>();
        boolean inAssignmentSection = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Pomijaj puste linie i komentarze
                }
                
                // Sprawdź czy zaczynamy sekcję przypisań
                if (!inAssignmentSection && line.contains(" - ")) {
                    inAssignmentSection = true;
                }
                
                if (inAssignmentSection) {
                    assignmentLines.add(line);
                } else {
                    lines.add(line);
                }
            }
        }
        
        // Parsuj macierz sąsiedztwa
        int n = lines.size();
        boolean[][] adjacencyMatrix = new boolean[n][n];
        
        for (int i = 0; i < n; i++) {
            String line = lines.get(i);
            // Usuń nawiasy kwadratowe jeśli są
            line = line.replaceAll("[\\[\\]]", "");
            
            String[] values = line.split("[\\s.]+");
            for (int j = 0; j < Math.min(values.length, n); j++) {
                String val = values[j].trim();
                if (!val.isEmpty()) {
                    adjacencyMatrix[i][j] = "1".equals(val) || "1.0".equals(val);
                }
            }
        }
        
        // Konwertuj macierz sąsiedztwa na format CSR
        List<Integer> adjacencyList = new ArrayList<>();
        int[] rowPointers = new int[n + 1];
        int edgeCount = 0;
        
        for (int i = 0; i < n; i++) {
            rowPointers[i] = adjacencyList.size();
            for (int j = 0; j < n; j++) {
                if (adjacencyMatrix[i][j]) {
                    adjacencyList.add(j);
                    if (i < j) { // Liczymy każdą krawędź tylko raz dla grafu nieskierowanego
                        edgeCount++;
                    }
                }
            }
        }
        rowPointers[n] = adjacencyList.size();
        
        // Stwórz graf
        Graph graph = new Graph(n, edgeCount, rowPointers, 
                               adjacencyList.stream().mapToInt(Integer::intValue).toArray());
        
        // Parsuj przypisania jeśli istnieją
        Partition partition = null;
        if (!assignmentLines.isEmpty()) {
            int[] assignments = new int[n];
            Arrays.fill(assignments, -1);
            int maxPartId = -1;
            
            for (String line : assignmentLines) {
                String[] parts = line.split(" - ");
                if (parts.length == 2) {
                    try {
                        int vertex = Integer.parseInt(parts[0].trim());
                        int partId = Integer.parseInt(parts[1].trim());
                        if (vertex >= 0 && vertex < n) {
                            assignments[vertex] = partId;
                            maxPartId = Math.max(maxPartId, partId);
                        }
                    } catch (NumberFormatException e) {
                        // Ignoruj nieprawidłowe linie
                    }
                }
            }
            
            if (maxPartId >= 0) {
                int partCount = maxPartId + 1;
                int[] partSizes = new int[partCount];
                for (int assignment : assignments) {
                    if (assignment >= 0) {
                        partSizes[assignment]++;
                    }
                }
                
                partition = new Partition(assignments, partSizes, partCount, 0, 10);
                // Oblicz przecięte krawędzie zostanie zrobione później
            }
        }
        
        return new Pair<>(graph, partition);
    }
    
    /**
     * Format 2: CSRRG (tekstowy)
     * Wczytuje tylko graf główny (linie 4-5)
     */
    public static Pair<Graph, CsrrgDataHolder> loadGraphFromCsrrgText(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Linia 1: max secondary value
            String line1 = reader.readLine();
            int maxSecondaryValue = Integer.parseInt(line1.trim());
            
            // Linia 2: secondary data
            String line2 = reader.readLine();
            int[] secondaryData = parseIntegerList(line2, ";");
            
            // Linia 3: secondary row pointers
            String line3 = reader.readLine();
            int[] secondaryRowPtr = parseIntegerList(line3, ";");
            
            // Linia 4: graph neighbors (lista sąsiedztwa)
            String line4 = reader.readLine();
            int[] graphNeighbors = parseIntegerList(line4, ";");
            
            // Linia 5: graph row pointers
            String line5 = reader.readLine();
            int[] graphRowPtr = parseIntegerList(line5, ";");
            
            // Walidacja danych
            if (graphRowPtr.length < 2) {
                throw new IOException("Invalid graph row pointers");
            }
            
            int vertexCount = graphRowPtr.length - 1;
            int edgeCount = graphNeighbors.length / 2; // Dla grafu nieskierowanego
            
            // Stwórz graf
            Graph graph = new Graph(vertexCount, edgeCount, graphRowPtr, graphNeighbors);
            
            // Stwórz holder dla danych CSRRG
            CsrrgDataHolder csrrgData = new CsrrgDataHolder(maxSecondaryValue, secondaryData, 
                                                            secondaryRowPtr, graphNeighbors, graphRowPtr);
            
            return new Pair<>(graph, csrrgData);
        }
    }
    
    /**
     * Format 3: "Proste Przypisanie" Tekstowe
     * Linie w formacie: Wierzchołek X -> Podgraf Y
     */
    public static Pair<Graph, Partition> loadSimpleAssignmentText(String filePath) throws IOException {
        int maxVertexId = -1;
        int maxPartId = -1;
        Map<Integer, Integer> assignments = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Parsuj linię w formacie "Wierzchołek X -> Podgraf Y"
                if (line.startsWith("Wierzchołek") && line.contains("->") && line.contains("Podgraf")) {
                    String[] parts = line.split("->");
                    if (parts.length == 2) {
                        try {
                            // Wyciągnij numer wierzchołka
                            String vertexPart = parts[0].trim();
                            int vertexStart = vertexPart.indexOf(' ') + 1;
                            int vertexId = Integer.parseInt(vertexPart.substring(vertexStart).trim());
                            
                            // Wyciągnij numer podgrafu
                            String partPart = parts[1].trim();
                            int partStart = partPart.indexOf(' ') + 1;
                            int partId = Integer.parseInt(partPart.substring(partStart).trim());
                            
                            assignments.put(vertexId, partId);
                            maxVertexId = Math.max(maxVertexId, vertexId);
                            maxPartId = Math.max(maxPartId, partId);
                        } catch (Exception e) {
                            // Ignoruj nieprawidłowe linie
                        }
                    }
                }
            }
        }
        
        // Stwórz graf bez krawędzi
        int vertexCount = maxVertexId + 1;
        int[] rowPointers = new int[vertexCount + 1];
        Arrays.fill(rowPointers, 0);
        int[] adjacencyList = new int[0];
        
        Graph graph = new Graph(vertexCount, 0, rowPointers, adjacencyList);
        
        // Stwórz podział
        int partCount = maxPartId + 1;
        int[] assignmentsArray = new int[vertexCount];
        Arrays.fill(assignmentsArray, 0); // Domyślnie część 0
        int[] partSizes = new int[partCount];
        
        for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
            assignmentsArray[entry.getKey()] = entry.getValue();
            partSizes[entry.getValue()]++;
        }
        
        // Uzupełnij rozmiary części dla wierzchołków bez przypisania
        for (int i = 0; i < vertexCount; i++) {
            if (!assignments.containsKey(i)) {
                partSizes[0]++;
            }
        }
        
        Partition partition = new Partition(assignmentsArray, partSizes, partCount, 0, 10);
        
        return new Pair<>(graph, partition);
    }
    
    /**
     * Format 4: "Proste Przypisanie" Binarne
     * Pary (int vertexId, int partId) w formacie big-endian
     */
    public static Pair<Graph, Partition> loadSimpleAssignmentBinary(String filePath) throws IOException {
        int maxVertexId = -1;
        int maxPartId = -1;
        Map<Integer, Integer> assignments = new HashMap<>();
        
        try (DataInputStream dis = new DataInputStream(new FileInputStream(filePath))) {
            while (dis.available() >= 8) { // 2 * 4 bajty
                int vertexId = dis.readInt();
                int partId = dis.readInt();
                
                assignments.put(vertexId, partId);
                maxVertexId = Math.max(maxVertexId, vertexId);
                maxPartId = Math.max(maxPartId, partId);
            }
        }
        
        // Stwórz graf bez krawędzi
        int vertexCount = maxVertexId + 1;
        int[] rowPointers = new int[vertexCount + 1];
        Arrays.fill(rowPointers, 0);
        int[] adjacencyList = new int[0];
        
        Graph graph = new Graph(vertexCount, 0, rowPointers, adjacencyList);
        
        // Stwórz podział
        int partCount = maxPartId + 1;
        int[] assignmentsArray = new int[vertexCount];
        Arrays.fill(assignmentsArray, 0); // Domyślnie część 0
        int[] partSizes = new int[partCount];
        
        for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
            assignmentsArray[entry.getKey()] = entry.getValue();
            partSizes[entry.getValue()]++;
        }
        
        // Uzupełnij rozmiary części dla wierzchołków bez przypisania
        for (int i = 0; i < vertexCount; i++) {
            if (!assignments.containsKey(i)) {
                partSizes[0]++;
            }
        }
        
        Partition partition = new Partition(assignmentsArray, partSizes, partCount, 0, 10);
        
        return new Pair<>(graph, partition);
    }
}