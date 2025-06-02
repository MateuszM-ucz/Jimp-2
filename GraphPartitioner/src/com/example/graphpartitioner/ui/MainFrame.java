package com.example.graphpartitioner.ui;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;
import com.example.graphpartitioner.io.*;
import com.example.graphpartitioner.utils.Pair;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Główne okno aplikacji
 */
public class MainFrame extends JFrame {
    private GraphPanel graphPanel;
    private ToolPanel toolPanel;
    private Graph currentGraph;
    private Partition currentPartition;
    private CsrrgDataHolder loadedCsrrgData;
    
    private JLabel statusLabel;
    private JFileChooser fileChooser;
    
    public MainFrame() {
        initializeComponents();
        layoutComponents();
        createMenuBar();
        
        setTitle("Graph Partitioner - Wizualizacja i partycjonowanie grafów");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
    
    private void initializeComponents() {
        graphPanel = new GraphPanel();
        toolPanel = new ToolPanel(this);
        statusLabel = new JLabel("Gotowy. Wczytaj graf z menu Plik.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Panel główny z grafem
        JScrollPane scrollPane = new JScrollPane(graphPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Wizualizacja grafu"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel narzędzi po prawej
        add(toolPanel, BorderLayout.EAST);
        
        // Pasek statusu na dole
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Menu Plik
        JMenu fileMenu = new JMenu("Plik");
        fileMenu.setMnemonic('P');
        
        // Opcje wczytywania
        JMenuItem loadTextCSR = new JMenuItem("Wczytaj tekstowy CSR/Macierz...");
        loadTextCSR.addActionListener(e -> loadTextCSRGraph());
        fileMenu.add(loadTextCSR);
        
        JMenuItem loadCSRRG = new JMenuItem("Wczytaj CSRRG...");
        loadCSRRG.addActionListener(e -> loadCSRRGGraph());
        fileMenu.add(loadCSRRG);
        
        JMenuItem loadSimpleText = new JMenuItem("Wczytaj proste przypisanie (tekst)...");
        loadSimpleText.addActionListener(e -> loadSimpleAssignmentText());
        fileMenu.add(loadSimpleText);
        
        JMenuItem loadSimpleBinary = new JMenuItem("Wczytaj proste przypisanie (binarny)...");
        loadSimpleBinary.addActionListener(e -> loadSimpleAssignmentBinary());
        fileMenu.add(loadSimpleBinary);
        
        fileMenu.addSeparator();
        
        // Opcje zapisywania
        JMenuItem saveAsText = new JMenuItem("Zapisz podział jako tekst...");
        saveAsText.addActionListener(e -> savePartitionAsText());
        fileMenu.add(saveAsText);
        
        JMenuItem saveAsCSRRG = new JMenuItem("Zapisz podział jako CSRRG...");
        saveAsCSRRG.addActionListener(e -> savePartitionAsCSRRG());
        fileMenu.add(saveAsCSRRG);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Wyjście");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        
        // Menu Widok
        JMenu viewMenu = new JMenu("Widok");
        viewMenu.setMnemonic('W');
        
        JMenuItem resetView = new JMenuItem("Resetuj widok");
        resetView.addActionListener(e -> graphPanel.resetView());
        viewMenu.add(resetView);
        
        JMenuItem fitToWindow = new JMenuItem("Dopasuj do okna");
        fitToWindow.addActionListener(e -> graphPanel.fitToWindow());
        viewMenu.add(fitToWindow);
        
        menuBar.add(viewMenu);
        
        // Menu Pomoc
        JMenu helpMenu = new JMenu("Pomoc");
        helpMenu.setMnemonic('O');
        
        JMenuItem about = new JMenuItem("O programie");
        about.addActionListener(e -> showAboutDialog());
        helpMenu.add(about);
        
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void loadTextCSRGraph() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki tekstowe", "txt"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                statusLabel.setText("Wczytywanie grafu z " + file.getName() + "...");
                Pair<Graph, Partition> result = GraphLoader.loadGraphFromAdjacencyMatrixText(file.getAbsolutePath());
                
                currentGraph = result.getFirst();
                currentPartition = result.getSecond();
                loadedCsrrgData = null;
                
                displayGraphAndPartition(currentGraph, currentPartition);
                
                String message = "Wczytano graf: " + currentGraph.getVertexCount() + " wierzchołków, " +
                               currentGraph.getEdgeCount() + " krawędzi";
                if (currentPartition != null) {
                    message += ", " + currentPartition.getPartCount() + " części";
                }
                statusLabel.setText(message);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd wczytywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd wczytywania pliku");
            }
        }
    }
    
    private void loadCSRRGGraph() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki CSRRG", "csrrg", "txt"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                statusLabel.setText("Wczytywanie grafu CSRRG z " + file.getName() + "...");
                Pair<Graph, CsrrgDataHolder> result = GraphLoader.loadGraphFromCsrrgText(file.getAbsolutePath());
                
                currentGraph = result.getFirst();
                currentPartition = null;
                loadedCsrrgData = result.getSecond();
                
                displayGraphAndPartition(currentGraph, currentPartition);
                
                statusLabel.setText("Wczytano graf CSRRG: " + currentGraph.getVertexCount() + 
                                  " wierzchołków, " + currentGraph.getEdgeCount() + " krawędzi");
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd wczytywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd wczytywania pliku");
            }
        }
    }
    
    private void loadSimpleAssignmentText() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki tekstowe", "txt"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                statusLabel.setText("Wczytywanie przypisania z " + file.getName() + "...");
                Pair<Graph, Partition> result = GraphLoader.loadSimpleAssignmentText(file.getAbsolutePath());
                
                currentGraph = result.getFirst();
                currentPartition = result.getSecond();
                loadedCsrrgData = null;
                
                displayGraphAndPartition(currentGraph, currentPartition);
                
                statusLabel.setText("Wczytano przypisanie: " + currentGraph.getVertexCount() + 
                                  " wierzchołków, " + currentPartition.getPartCount() + " części");
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd wczytywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd wczytywania pliku");
            }
        }
    }
    
    private void loadSimpleAssignmentBinary() {
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki binarne", "bin"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                statusLabel.setText("Wczytywanie binarnego przypisania z " + file.getName() + "...");
                Pair<Graph, Partition> result = GraphLoader.loadSimpleAssignmentBinary(file.getAbsolutePath());
                
                currentGraph = result.getFirst();
                currentPartition = result.getSecond();
                loadedCsrrgData = null;
                
                displayGraphAndPartition(currentGraph, currentPartition);
                
                statusLabel.setText("Wczytano binarne przypisanie: " + currentGraph.getVertexCount() + 
                                  " wierzchołków, " + currentPartition.getPartCount() + " części");
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd wczytywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd wczytywania pliku");
            }
        }
    }
    
    private void savePartitionAsText() {
        if (currentGraph == null || currentPartition == null) {
            JOptionPane.showMessageDialog(this, 
                "Brak grafu lub podziału do zapisania", 
                "Uwaga", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki tekstowe", "txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Dodaj rozszerzenie jeśli brak
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            try {
                statusLabel.setText("Zapisywanie podziału do " + file.getName() + "...");
                GraphSaver.savePartitionToAdjacencyMatrixText(file.getAbsolutePath(), currentGraph, currentPartition);
                statusLabel.setText("Zapisano podział do " + file.getName());
                
                JOptionPane.showMessageDialog(this, 
                    "Podział został zapisany pomyślnie", 
                    "Sukces", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd zapisywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd zapisywania pliku");
            }
        }
    }
    
    private void savePartitionAsCSRRG() {
        if (currentGraph == null || currentPartition == null) {
            JOptionPane.showMessageDialog(this, 
                "Brak grafu lub podziału do zapisania", 
                "Uwaga", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter("Pliki CSRRG", "bin", "csrrg"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Dodaj rozszerzenie jeśli brak
            if (!file.getName().endsWith(".bin") && !file.getName().endsWith(".csrrg")) {
                file = new File(file.getAbsolutePath() + ".bin");
            }
            
            try {
                statusLabel.setText("Zapisywanie podziału CSRRG do " + file.getName() + "...");
                GraphSaver.savePartitionToCsrrgText(file.getAbsolutePath(), currentGraph, 
                                                   currentPartition, loadedCsrrgData);
                statusLabel.setText("Zapisano podział CSRRG do " + file.getName());
                
                JOptionPane.showMessageDialog(this, 
                    "Podział został zapisany pomyślnie w formacie CSRRG", 
                    "Sukces", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Błąd zapisywania pliku: " + ex.getMessage(), 
                    "Błąd", 
                    JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Błąd zapisywania pliku");
            }
        }
    }
    
    private void showAboutDialog() {
        String message = "Graph Partitioner\n\n" +
                        "Aplikacja do wizualizacji i partycjonowania grafów\n" +
                        "wykorzystująca algorytm Kernighana-Lina\n\n" +
                        "Wersja: 1.0\n" +
                        "Autor: Zespół Graph Partitioner\n\n" +
                        "Obsługiwane formaty:\n" +
                        "- Tekstowy CSR z macierzą sąsiedztwa\n" +
                        "- CSRRG (Compressed Sparse Row Row Graph)\n" +
                        "- Proste przypisanie (tekstowe i binarne)";
                        
        JOptionPane.showMessageDialog(this, message, "O programie", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Wyświetla graf i podział w panelu
     */
    private void displayGraphAndPartition(Graph graph, Partition partition) {
        graphPanel.setGraph(graph, partition);
        toolPanel.updateGraphInfo(graph, partition);
    }
    
    /**
     * Aktualizuje podział (wywoływane z ToolPanel)
     */
    public void updatePartition(Partition partition) {
        this.currentPartition = partition;
        graphPanel.setGraph(currentGraph, currentPartition);
        
        if (partition != null) {
            statusLabel.setText("Podział zaktualizowany: " + partition.getPartCount() + 
                              " części, " + partition.getCutEdges() + " przeciętych krawędzi");
        }
    }
    
    // Gettery
    public Graph getCurrentGraph() {
        return currentGraph;
    }
    
    public Partition getCurrentPartition() {
        return currentPartition;
    }
}