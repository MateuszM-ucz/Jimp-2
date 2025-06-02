package com.example.graphpartitioner.algorithms;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;

/**
 * Algorytm hybrydowy łączący różne metody inicjalizacji i optymalizacji
 */
public class HybridAlgorithm {
    
    /**
     * Oblicza adaptacyjną liczbę prób dla metody losowej
     */
    private static int calculateAdaptiveRandomTrials(Graph graph, int numParts, int currentBestCutEdges) {
        // Oblicz gęstość grafu
        double density = graph.getDensity();
        
        // Dla rzadszych grafów, próbuj więcej losowych inicjalizacji
        int baseTrials = 3; // domyślnie
        
        if (density < 0.01) {
            baseTrials = 5;
        } else if (density < 0.1) {
            baseTrials = 4;
        }
        
        // Dla większej liczby części, próbuj więcej inicjalizacji
        if (numParts > 10) {
            baseTrials += 2;
        } else if (numParts > 5) {
            baseTrials += 1;
        }
        
        // Jeśli nie znaleźliśmy dobrego rozwiązania, próbuj więcej
        if (currentBestCutEdges == Integer.MAX_VALUE || 
            currentBestCutEdges > graph.getEdgeCount() / 2) {
            baseTrials += 1;
        }
        
        System.out.println("Obliczono liczbę prób adaptacyjnie: " + baseTrials + 
                         " (gęstość grafu: " + String.format("%.4f", density) + 
                         ", liczba części: " + numParts + ")");
        
        return baseTrials;
    }
    
    /**
     * Główna funkcja znajdująca najlepszy podział za pomocą metod hybrydowych
     */
    public static Partition findBestPartitionHybrid(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0 || marginPercent < 0) {
            throw new IllegalArgumentException("Invalid parameters for hybrid algorithm");
        }
        
        Partition bestPartition = null;
        int bestCutEdges = Integer.MAX_VALUE;
        
        System.out.println("Szukanie optymalnego podziału grafu metodą hybrydową...");
        
        // 1. Najpierw wypróbuj deterministyczne strategie
        System.out.println("Krok 1: Wypróbowywanie deterministycznych strategii...");
        
        // 1.1 Strategia modulo
        System.out.println("  - Strategia modulo...");
        Partition moduloPartition = PartitionInitializer.initializeModulo(graph, numParts, marginPercent);
        if (moduloPartition != null) {
            System.out.println("    Początkowa liczba przeciętych krawędzi: " + moduloPartition.getCutEdges());
            KernighanLin.optimizeWithKernighanLin(graph, moduloPartition, 0);
            System.out.println("    Końcowa liczba przeciętych krawędzi: " + moduloPartition.getCutEdges());
            
            if (moduloPartition.getCutEdges() < bestCutEdges) {
                bestCutEdges = moduloPartition.getCutEdges();
                bestPartition = moduloPartition;
            }
        }
        
        // 1.2 Strategia sekwencyjna
        System.out.println("  - Strategia sekwencyjna...");
        Partition sequentialPartition = PartitionInitializer.initializeSequential(graph, numParts, marginPercent);
        if (sequentialPartition != null) {
            System.out.println("    Początkowa liczba przeciętych krawędzi: " + sequentialPartition.getCutEdges());
            KernighanLin.optimizeWithKernighanLin(graph, sequentialPartition, 0);
            System.out.println("    Końcowa liczba przeciętych krawędzi: " + sequentialPartition.getCutEdges());
            
            if (sequentialPartition.getCutEdges() < bestCutEdges) {
                bestCutEdges = sequentialPartition.getCutEdges();
                bestPartition = sequentialPartition;
            }
        }
        
        // 1.3 Strategia DFS (dodatkowa)
        System.out.println("  - Strategia DFS...");
        Partition dfsPartition = PartitionInitializer.initializeDFS(graph, numParts, marginPercent);
        if (dfsPartition != null) {
            System.out.println("    Początkowa liczba przeciętych krawędzi: " + dfsPartition.getCutEdges());
            KernighanLin.optimizeWithKernighanLin(graph, dfsPartition, 0);
            System.out.println("    Końcowa liczba przeciętych krawędzi: " + dfsPartition.getCutEdges());
            
            if (dfsPartition.getCutEdges() < bestCutEdges) {
                bestCutEdges = dfsPartition.getCutEdges();
                bestPartition = dfsPartition;
            }
        }
        
        // 2. Następnie wypróbuj losowe inicjalizacje z adaptacyjną liczbą prób
        System.out.println("Krok 2: Wypróbowywanie losowych inicjalizacji...");
        
        // Oblicz adaptacyjnie liczbę losowych inicjalizacji do wypróbowania
        int randomTrials = calculateAdaptiveRandomTrials(graph, numParts, bestCutEdges);
        
        // Dla dużych grafów ograniczamy liczbę prób
        if (graph.getVertexCount() > 10000) {
            randomTrials = Math.min(randomTrials, 2);
            System.out.println("Duży graf (" + graph.getVertexCount() + 
                             " wierzchołków) - ograniczenie do " + randomTrials + " prób losowych");
        }
        
        for (int seed = 0; seed < randomTrials; seed++) {
            System.out.println("  - Losowa inicjalizacja " + (seed + 1) + "/" + randomTrials + "...");
            Partition randomPartition = PartitionInitializer.initializeRandom(graph, numParts, marginPercent);
            if (randomPartition != null) {
                System.out.println("    Początkowa liczba przeciętych krawędzi: " + randomPartition.getCutEdges());
                KernighanLin.optimizeWithKernighanLin(graph, randomPartition, 0);
                System.out.println("    Końcowa liczba przeciętych krawędzi: " + randomPartition.getCutEdges());
                
                if (randomPartition.getCutEdges() < bestCutEdges) {
                    bestCutEdges = randomPartition.getCutEdges();
                    bestPartition = randomPartition;
                }
            }
        }
        
        // 3. Na koniec, spróbuj perturbacji najlepszego znalezionego rozwiązania
        if (bestPartition != null) {
            System.out.println("Krok 3: Testowanie perturbacji najlepszego rozwiązania...");
            
            // Określ liczbę perturbacji do wypróbowania
            int numPerturbations = 2;
            if (graph.getVertexCount() > 1000) numPerturbations = 3;
            
            for (int i = 0; i < numPerturbations; i++) {
                System.out.println("  - Perturbacja " + (i + 1) + "/" + numPerturbations + "...");
                
                // Użyj wyższego współczynnika perturbacji dla pierwszej próby
                double perturbationRatio = (i == 0) ? 0.15 : 0.1;
                
                // Naprzemiennie używaj zwykłej i inteligentnej perturbacji
                Partition perturbed;
                if (i % 2 == 0) {
                    perturbed = Perturbation.perturbPartition(bestPartition, graph, perturbationRatio);
                } else {
                    perturbed = Perturbation.perturbPartitionSmart(bestPartition, graph, perturbationRatio);
                }
                
                if (perturbed != null) {
                    System.out.println("    Początkowa liczba przeciętych krawędzi po perturbacji: " + 
                                     perturbed.getCutEdges());
                    KernighanLin.optimizeWithKernighanLin(graph, perturbed, 0);
                    System.out.println("    Końcowa liczba przeciętych krawędzi: " + perturbed.getCutEdges());
                    
                    if (perturbed.getCutEdges() < bestCutEdges) {
                        bestCutEdges = perturbed.getCutEdges();
                        bestPartition = perturbed;
                        System.out.println("    Znaleziono nowe najlepsze rozwiązanie!");
                    }
                }
            }
        }
        
        if (bestPartition != null) {
            System.out.println("Najlepszy znaleziony podział ma " + bestCutEdges + " przeciętych krawędzi.");
            
            // Weryfikacja poprawności końcowego podziału
            int verification = PartitionUtils.calculateCutEdges(graph, bestPartition);
            if (verification != bestPartition.getCutEdges()) {
                System.out.println("Końcowa weryfikacja: korekta liczby przeciętych krawędzi z " + 
                                 bestPartition.getCutEdges() + " na " + verification);
                bestPartition.setCutEdges(verification);
            }
        } else {
            System.err.println("Nie udało się znaleźć żadnego podziału!");
        }
        
        return bestPartition;
    }
    
    /**
     * Uproszczona wersja algorytmu hybrydowego (bez komunikatów)
     */
    public static Partition findBestPartitionHybridSilent(Graph graph, int numParts, int marginPercent) {
        if (graph == null || graph.getVertexCount() <= 0 || numParts <= 0 || marginPercent < 0) {
            return null;
        }
        
        Partition bestPartition = null;
        int bestCutEdges = Integer.MAX_VALUE;
        
        // 1. Strategie deterministyczne
        Partition[] deterministicStrategies = {
            PartitionInitializer.initializeModulo(graph, numParts, marginPercent),
            PartitionInitializer.initializeSequential(graph, numParts, marginPercent),
            PartitionInitializer.initializeDFS(graph, numParts, marginPercent)
        };
        
        for (Partition partition : deterministicStrategies) {
            if (partition != null) {
                KernighanLin.optimizeWithKernighanLinSilent(graph, partition, 0);
                if (partition.getCutEdges() < bestCutEdges) {
                    bestCutEdges = partition.getCutEdges();
                    bestPartition = partition;
                }
            }
        }
        
        // 2. Losowe inicjalizacje
        int randomTrials = calculateAdaptiveRandomTrials(graph, numParts, bestCutEdges);
        randomTrials = Math.min(randomTrials, graph.getVertexCount() > 10000 ? 2 : randomTrials);
        
        for (int i = 0; i < randomTrials; i++) {
            Partition randomPartition = PartitionInitializer.initializeRandom(graph, numParts, marginPercent);
            if (randomPartition != null) {
                KernighanLin.optimizeWithKernighanLinSilent(graph, randomPartition, 0);
                if (randomPartition.getCutEdges() < bestCutEdges) {
                    bestCutEdges = randomPartition.getCutEdges();
                    bestPartition = randomPartition;
                }
            }
        }
        
        // 3. Perturbacje
        if (bestPartition != null) {
            int numPerturbations = graph.getVertexCount() > 1000 ? 3 : 2;
            
            for (int i = 0; i < numPerturbations; i++) {
                double ratio = (i == 0) ? 0.15 : 0.1;
                Partition perturbed = (i % 2 == 0) ? 
                    Perturbation.perturbPartition(bestPartition, graph, ratio) :
                    Perturbation.perturbPartitionSmart(bestPartition, graph, ratio);
                
                if (perturbed != null) {
                    KernighanLin.optimizeWithKernighanLinSilent(graph, perturbed, 0);
                    if (perturbed.getCutEdges() < bestCutEdges) {
                        bestCutEdges = perturbed.getCutEdges();
                        bestPartition = perturbed;
                    }
                }
            }
        }
        
        // Weryfikacja końcowa
        if (bestPartition != null) {
            bestPartition.setCutEdges(PartitionUtils.calculateCutEdges(graph, bestPartition));
        }
        
        return bestPartition;
    }
}