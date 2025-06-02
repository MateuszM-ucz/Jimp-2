package com.example.graphpartitioner.ui;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;
import com.example.graphpartitioner.algorithms.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Panel narzędzi do konfiguracji parametrów partycjonowania
 */
public class ToolPanel extends JPanel {
    private final MainFrame mainFrame;
    
    // Kontrolki
    private JSpinner numPartsSpinner;
    private JSpinner marginSpinner;
    private JComboBox<String> algorithmComboBox;
    private JCheckBox useHybridCheckBox;
    private JButton partitionButton;
    private JButton resetButton;
    
    // Etykiety informacyjne
    private JLabel graphInfoLabel;
    private JLabel partitionInfoLabel;
    private JTextArea detailsTextArea;
    
    public ToolPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        setPreferredSize(new Dimension(300, 600));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        initializeComponents();
        layoutComponents();
    }
    
    private void initializeComponents() {
        // Spinner dla liczby części
        SpinnerNumberModel numPartsModel = new SpinnerNumberModel(2, 2, 100, 1);
        numPartsSpinner = new JSpinner(numPartsModel);
        
        // Spinner dla marginesu procentowego
        SpinnerNumberModel marginModel = new SpinnerNumberModel(10, 0, 100, 5);
        marginSpinner = new JSpinner(marginModel);
        
        // ComboBox dla algorytmu
        String[] algorithms = {"Modulo", "Sekwencyjny", "Losowy", "DFS"};
        algorithmComboBox = new JComboBox<>(algorithms);
        algorithmComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // CheckBox dla algorytmu hybrydowego
        useHybridCheckBox = new JCheckBox("Użyj algorytmu hybrydowego");
        useHybridCheckBox.setToolTipText("Wypróbuje różne strategie i wybierze najlepszą");
        
        // Przyciski
        partitionButton = new JButton("Partycjonuj graf");
        partitionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        partitionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        partitionButton.setEnabled(false);
        
        resetButton = new JButton("Resetuj podział");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        resetButton.setEnabled(false);
        
        // Etykiety informacyjne
        graphInfoLabel = new JLabel("Brak wczytanego grafu");
        partitionInfoLabel = new JLabel(" ");
        
        // Obszar szczegółów
        detailsTextArea = new JTextArea(8, 20);
        detailsTextArea.setEditable(false);
        detailsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        
        // Listenery
        useHybridCheckBox.addActionListener(e -> {
            algorithmComboBox.setEnabled(!useHybridCheckBox.isSelected());
        });
        
        partitionButton.addActionListener(e -> performPartitioning());
        resetButton.addActionListener(e -> resetPartition());
    }
    
    private void layoutComponents() {
        // Panel informacji o grafie
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Informacje o grafie"));
        infoPanel.add(graphInfoLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(partitionInfoLabel);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        // Panel parametrów
        JPanel parametersPanel = new JPanel(new GridBagLayout());
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parametry partycjonowania"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Liczba części
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        parametersPanel.add(new JLabel("Liczba części:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        parametersPanel.add(numPartsSpinner, gbc);
        
        // Margines procentowy
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        parametersPanel.add(new JLabel("Margines (%):"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        parametersPanel.add(marginSpinner, gbc);
        
        // Algorytm
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        parametersPanel.add(new JLabel("Algorytm:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        parametersPanel.add(algorithmComboBox, gbc);
        
        // Checkbox hybrydowy
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        parametersPanel.add(useHybridCheckBox, gbc);
        
        parametersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Panel przycisków
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(partitionButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(resetButton);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // Panel szczegółów
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Szczegóły"));
        JScrollPane scrollPane = new JScrollPane(detailsTextArea);
        detailsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Dodaj wszystkie komponenty do głównego panelu
        add(infoPanel);
        add(Box.createVerticalStrut(10));
        add(parametersPanel);
        add(Box.createVerticalStrut(10));
        add(buttonPanel);
        add(Box.createVerticalStrut(10));
        add(detailsPanel);
        add(Box.createVerticalGlue());
    }
    
    /**
     * Aktualizuje informacje o grafie
     */
    public void updateGraphInfo(Graph graph, Partition partition) {
        if (graph != null) {
            graphInfoLabel.setText(String.format("Wierzchołki: %d, Krawędzie: %d", 
                                               graph.getVertexCount(), graph.getEdgeCount()));
            partitionButton.setEnabled(true);
            
            if (partition != null) {
                partitionInfoLabel.setText(String.format("Części: %d, Przecięte: %d", 
                                                       partition.getPartCount(), 
                                                       partition.getCutEdges()));
                resetButton.setEnabled(true);
                updateDetailsPanel(graph, partition);
            } else {
                partitionInfoLabel.setText("Brak podziału");
                resetButton.setEnabled(false);
                detailsTextArea.setText("Graf nie został jeszcze podzielony.");
            }
        } else {
            graphInfoLabel.setText("Brak wczytanego grafu");
            partitionInfoLabel.setText(" ");
            partitionButton.setEnabled(false);
            resetButton.setEnabled(false);
            detailsTextArea.setText("");
        }
    }
    
    /**
     * Aktualizuje panel szczegółów
     */
    private void updateDetailsPanel(Graph graph, Partition partition) {
        StringBuilder sb = new StringBuilder();
        
        // Podstawowe statystyki
        sb.append("STATYSTYKI PODZIAŁU\n");
        sb.append("===================\n\n");
        
        // Rozmiary części
        sb.append("Rozmiary części:\n");
        for (int i = 0; i < partition.getPartCount(); i++) {
            sb.append(String.format("  Część %d: %d wierzchołków\n", 
                                  i, partition.getPartSizes()[i]));
        }
        
        // Balans
        sb.append("\nBalans:\n");
        int avgSize = partition.getAveragePartSize();
        int maxImbalance = partition.getMaxImbalance();
        sb.append(String.format("  Średni rozmiar: %d\n", avgSize));
        sb.append(String.format("  Max. nierówność: ±%d\n", maxImbalance));
        sb.append(String.format("  Status: %s\n", 
                              partition.isBalanced() ? "Zbalansowany" : "Niezbalansowany"));
        
        // Gęstość grafu
        sb.append("\nGęstość grafu:\n");
        sb.append(String.format("  %.2f%%\n", graph.getDensity() * 100));
        
        // Efektywność podziału
        sb.append("\nEfektywność:\n");
        double cutRatio = (double) partition.getCutEdges() / graph.getEdgeCount();
        sb.append(String.format("  Przecięte/Wszystkie: %.1f%%\n", cutRatio * 100));
        
        detailsTextArea.setText(sb.toString());
        detailsTextArea.setCaretPosition(0);
    }
    
    /**
     * Wykonuje partycjonowanie grafu
     */
    private void performPartitioning() {
        Graph graph = mainFrame.getCurrentGraph();
        if (graph == null) {
            JOptionPane.showMessageDialog(this, 
                "Najpierw wczytaj graf!", 
                "Błąd", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Pobierz parametry
        int numParts = (Integer) numPartsSpinner.getValue();
        int marginPercent = (Integer) marginSpinner.getValue();
        
        // Walidacja
        if (numParts > graph.getVertexCount()) {
            JOptionPane.showMessageDialog(this, 
                "Liczba części nie może być większa niż liczba wierzchołków!", 
                "Błąd", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Ustaw kursor oczekiwania
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        partitionButton.setEnabled(false);
        
        // Wykonaj partycjonowanie w osobnym wątku
        SwingWorker<Partition, String> worker = new SwingWorker<Partition, String>() {
            @Override
            protected Partition doInBackground() throws Exception {
                publish("Rozpoczynam partycjonowanie...");
                
                Partition partition;
                
                if (useHybridCheckBox.isSelected()) {
                    publish("Używam algorytmu hybrydowego...");
                    partition = HybridAlgorithm.findBestPartitionHybrid(graph, numParts, marginPercent);
                } else {
                    String algorithm = (String) algorithmComboBox.getSelectedItem();
                    publish("Inicjalizacja metodą: " + algorithm);
                    
                    switch (algorithm) {
                        case "Modulo":
                            partition = PartitionInitializer.initializeModulo(graph, numParts, marginPercent);
                            break;
                        case "Sekwencyjny":
                            partition = PartitionInitializer.initializeSequential(graph, numParts, marginPercent);
                            break;
                        case "Losowy":
                            partition = PartitionInitializer.initializeRandom(graph, numParts, marginPercent);
                            break;
                        case "DFS":
                            partition = PartitionInitializer.initializeDFS(graph, numParts, marginPercent);
                            break;
                        default:
                            partition = PartitionInitializer.initializeModulo(graph, numParts, marginPercent);
                    }
                    
                    if (partition != null) {
                        publish("Optymalizacja algorytmem Kernighana-Lina...");
                        KernighanLin.optimizeWithKernighanLin(graph, partition, 0);
                    }
                }
                
                return partition;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    detailsTextArea.append(message + "\n");
                    detailsTextArea.setCaretPosition(detailsTextArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                try {
                    Partition partition = get();
                    
                    if (partition != null) {
                        mainFrame.updatePartition(partition);
                        updateGraphInfo(graph, partition);
                        
                        JOptionPane.showMessageDialog(ToolPanel.this,
                            String.format("Partycjonowanie zakończone!\n\n" +
                                        "Liczba części: %d\n" +
                                        "Przecięte krawędzie: %d",
                                        partition.getPartCount(), 
                                        partition.getCutEdges()),
                            "Sukces",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ToolPanel.this,
                            "Partycjonowanie nie powiodło się!",
                            "Błąd",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ToolPanel.this,
                        "Błąd podczas partycjonowania: " + e.getMessage(),
                        "Błąd",
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                    partitionButton.setEnabled(true);
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Resetuje podział grafu
     */
    private void resetPartition() {
        mainFrame.updatePartition(null);
        updateGraphInfo(mainFrame.getCurrentGraph(), null);
        detailsTextArea.setText("Podział został zresetowany.");
    }
}