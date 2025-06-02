package com.example.graphpartitioner.algorithms;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

import java.util.*;

/**
 * Klasa do wprowadzania perturbacji do istniejących podziałów
 */
public class Perturbation {
    
    /**
     * Wprowadza perturbacje do istniejącego podziału
     * @param source Źródłowy podział
     * @param graph Graf
     * @param perturbationRatio Współczynnik perturbacji (0.0 - 1.0)
     * @return Nowy podział z wprowadzonymi perturbacjami
     */
    public static Partition perturbPartition(Partition source, Graph graph, double perturbationRatio) {
        if (source == null || graph == null || 
            graph.getVertexCount() == 0 || source.getPartCount() <= 1) {
            System.err.println("Nieprawidłowe parametry w perturbPartition");
            return null;
        }
        
        // Stwórz kopię podziału
        Partition perturbed = source.copy(graph.getVertexCount());
        if (perturbed == null) return null;
        
        // Oblicz liczbę wierzchołków do perturbacji
        int verticesToPerturb = (int)(graph.getVertexCount() * perturbationRatio);
        if (verticesToPerturb < 1 && perturbationRatio > 0 && graph.getVertexCount() > 0) {
            verticesToPerturb = 1;
        }
        if (verticesToPerturb > graph.getVertexCount()) {
            verticesToPerturb = graph.getVertexCount();
        }
        
        System.out.println("Perturbacja: wykonywanie " + verticesToPerturb + " losowych ruchów...");
        
        Random random = new Random();
        int movesMade = 0;
        int maxAttempts = verticesToPerturb * 10; // Limit prób, aby uniknąć nieskończonej pętli
        int attempts = 0;
        
        // Lista wierzchołków do potencjalnego przeniesienia
        List<Integer> vertexList = new ArrayList<>();
        for (int i = 0; i < graph.getVertexCount(); i++) {
            vertexList.add(i);
        }
        Collections.shuffle(vertexList, random);
        
        while (movesMade < verticesToPerturb && attempts < maxAttempts) {
            attempts++;
            
            // Wybierz losowy wierzchołek
            int v = vertexList.get(attempts % vertexList.size());
            int srcPart = perturbed.getAssignment(v);
            
            // Znajdź losową część docelową różną od obecnej
            int destPart = srcPart;
            int partAttempts = 0;
            while (destPart == srcPart && perturbed.getPartCount() > 1 && partAttempts < 10) {
                destPart = random.nextInt(perturbed.getPartCount());
                partAttempts++;
            }
            
            if (destPart == srcPart) continue;
            
            // Sprawdź czy przeniesienie tego wierzchołka nie narusza ograniczeń równowagi
            if (!PartitionUtils.canMoveVertex(perturbed, v, destPart)) {
                continue;
            }
            
            // Przenieś wierzchołek
            perturbed.setAssignment(v, destPart);
            movesMade++;
        }
        
        System.out.println("Perturbacja: wykonano " + movesMade + "/" + verticesToPerturb + 
                         " ruchów po " + attempts + " próbach");
        
        // Zaktualizuj liczbę przeciętych krawędzi
        perturbed.setCutEdges(PartitionUtils.calculateCutEdges(graph, perturbed));
        
        return perturbed;
    }
    
    /**
     * Wprowadza inteligentne perturbacje skupiające się na wierzchołkach granicznych
     * (tych które mają sąsiadów w innych częściach)
     */
    public static Partition perturbPartitionSmart(Partition source, Graph graph, double perturbationRatio) {
        if (source == null || graph == null || 
            graph.getVertexCount() == 0 || source.getPartCount() <= 1) {
            return null;
        }
        
        // Stwórz kopię podziału
        Partition perturbed = source.copy(graph.getVertexCount());
        if (perturbed == null) return null;
        
        // Znajdź wierzchołki graniczne
        List<Integer> boundaryVertices = new ArrayList<>();
        for (int v = 0; v < graph.getVertexCount(); v++) {
            int currentPart = perturbed.getAssignment(v);
            boolean isBoundary = false;
            
            for (int neighbor : graph.getNeighbors(v)) {
                if (perturbed.getAssignment(neighbor) != currentPart) {
                    isBoundary = true;
                    break;
                }
            }
            
            if (isBoundary) {
                boundaryVertices.add(v);
            }
        }
        
        if (boundaryVertices.isEmpty()) {
            // Jeśli nie ma wierzchołków granicznych, użyj zwykłej perturbacji
            return perturbPartition(source, graph, perturbationRatio);
        }
        
        // Oblicz liczbę wierzchołków do perturbacji
        int verticesToPerturb = (int)(boundaryVertices.size() * perturbationRatio);
        verticesToPerturb = Math.max(1, Math.min(verticesToPerturb, boundaryVertices.size()));
        
        System.out.println("Inteligentna perturbacja: " + verticesToPerturb + 
                         " z " + boundaryVertices.size() + " wierzchołków granicznych");
        
        // Tasuj listę wierzchołków granicznych
        Random random = new Random();
        Collections.shuffle(boundaryVertices, random);
        
        int movesMade = 0;
        for (int i = 0; i < Math.min(verticesToPerturb, boundaryVertices.size()); i++) {
            int v = boundaryVertices.get(i);
            int currentPart = perturbed.getAssignment(v);
            
            // Znajdź część z największą liczbą sąsiadów
            Map<Integer, Integer> neighborCounts = new HashMap<>();
            for (int neighbor : graph.getNeighbors(v)) {
                int neighborPart = perturbed.getAssignment(neighbor);
                neighborCounts.put(neighborPart, 
                                  neighborCounts.getOrDefault(neighborPart, 0) + 1);
            }
            
            // Wybierz część docelową (preferuj tę z największą liczbą sąsiadów)
            int bestPart = -1;
            int maxNeighbors = -1;
            for (Map.Entry<Integer, Integer> entry : neighborCounts.entrySet()) {
                int part = entry.getKey();
                int count = entry.getValue();
                
                if (part != currentPart && count > maxNeighbors && 
                    PartitionUtils.canMoveVertex(perturbed, v, part)) {
                    bestPart = part;
                    maxNeighbors = count;
                }
            }
            
            // Jeśli nie znaleziono dobrej części, wybierz losową
            if (bestPart == -1) {
                List<Integer> validParts = new ArrayList<>();
                for (int p = 0; p < perturbed.getPartCount(); p++) {
                    if (p != currentPart && PartitionUtils.canMoveVertex(perturbed, v, p)) {
                        validParts.add(p);
                    }
                }
                
                if (!validParts.isEmpty()) {
                    bestPart = validParts.get(random.nextInt(validParts.size()));
                }
            }
            
            // Przenieś wierzchołek jeśli znaleziono docelową część
            if (bestPart != -1) {
                perturbed.setAssignment(v, bestPart);
                movesMade++;
            }
        }
        
        System.out.println("Inteligentna perturbacja: wykonano " + movesMade + " ruchów");
        
        // Zaktualizuj liczbę przeciętych krawędzi
        perturbed.setCutEdges(PartitionUtils.calculateCutEdges(graph, perturbed));
        
        return perturbed;
    }
}