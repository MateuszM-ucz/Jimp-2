package pl.edu.graph.ui;

import pl.edu.graph.jni.GraphPartitionerNative;
import pl.edu.graph.model.Graph;
import pl.edu.graph.model.Partition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class MainFrame extends JFrame {
    private GraphPanel graphPanel;
    private ToolPanel toolPanel;
    private Graph currentGraph;
    private Partition currentPartition;
    private GraphPartitionerNative nativeLib;
    
    public MainFrame() {
        setTitle("Graph Partitioner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        initComponents();
        
        // Load native library
        try {
            nativeLib = new GraphPartitionerNative();
            System.out.println("Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading native library: " + e.getMessage(),
                "Native Library Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void initComponents() {
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        
        JMenuItem loadTextItem = new JMenuItem("Load Text Graph");
        loadTextItem.addActionListener(this::loadTextGraph);
        fileMenu.add(loadTextItem);
        
        JMenuItem loadBinaryItem = new JMenuItem("Load Binary Graph");
        loadBinaryItem.addActionListener(this::loadBinaryGraph);
        fileMenu.add(loadBinaryItem);
        
        fileMenu.addSeparator();
        
        JMenuItem saveTextItem = new JMenuItem("Save as Text");
        saveTextItem.addActionListener(this::saveTextGraph);
        fileMenu.add(saveTextItem);
        
        JMenuItem saveBinaryItem = new JMenuItem("Save as Binary");
        saveBinaryItem.addActionListener(this::saveBinaryGraph);
        fileMenu.add(saveBinaryItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Graph display panel (center)
        graphPanel = new GraphPanel();
        mainPanel.add(new JScrollPane(graphPanel), BorderLayout.CENTER);
        
        // Tool panel (right side)
        toolPanel = new ToolPanel();
        toolPanel.setPartitionButtonAction(this::partitionGraph);
        mainPanel.add(toolPanel, BorderLayout.EAST);
        
        setContentPane(mainPanel);
    }
    
    private void loadTextGraph(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                currentGraph = nativeLib.loadGraph(file.getAbsolutePath());
                graphPanel.setGraph(currentGraph);
                toolPanel.setGraphLoaded(true);
                JOptionPane.showMessageDialog(this, 
                    "Graph loaded successfully: " + currentGraph.getVertexCount() + " vertices, " 
                    + currentGraph.getEdgeCount() + " edges",
                    "Graph Loaded", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading graph: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }
    
    private void loadBinaryGraph(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                currentGraph = nativeLib.loadGraph(file.getAbsolutePath());
                graphPanel.setGraph(currentGraph);
                toolPanel.setGraphLoaded(true);
                JOptionPane.showMessageDialog(this, 
                    "Graph loaded successfully: " + currentGraph.getVertexCount() + " vertices, " 
                    + currentGraph.getEdgeCount() + " edges",
                    "Graph Loaded", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading graph: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }
    
    private void saveTextGraph(ActionEvent e) {
        if (currentGraph == null || currentPartition == null) {
            JOptionPane.showMessageDialog(this, 
                "No graph or partition to save.",
                "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                nativeLib.savePartition(file.getAbsolutePath(), currentGraph, currentPartition, "txt");
                JOptionPane.showMessageDialog(this, 
                    "Graph partition saved successfully",
                    "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving graph: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }
    
    private void saveBinaryGraph(ActionEvent e) {
        if (currentGraph == null || currentPartition == null) {
            JOptionPane.showMessageDialog(this, 
                "No graph or partition to save.",
                "Save Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                nativeLib.savePartition(file.getAbsolutePath(), currentGraph, currentPartition, "bin");
                JOptionPane.showMessageDialog(this, 
                    "Graph partition saved successfully",
                    "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving graph: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }
    
    private void partitionGraph(ActionEvent e) {
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this, 
                "No graph loaded. Please load a graph first.",
                "Partition Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            int partCount = toolPanel.getPartitionCount();
            int marginPercent = toolPanel.getMarginPercent();
            String algorithm = toolPanel.getSelectedAlgorithm();
            boolean useHybrid = toolPanel.isHybridSelected();
            
            JOptionPane.showMessageDialog(this, 
                "Starting partitioning with: \n" +
                "Parts: " + partCount + "\n" +
                "Margin: " + marginPercent + "%\n" +
                "Algorithm: " + (useHybrid ? "Hybrid" : algorithm),
                "Partitioning", JOptionPane.INFORMATION_MESSAGE);
            
            currentPartition = nativeLib.partitionGraph(
                currentGraph, partCount, marginPercent, algorithm, useHybrid);
            
            graphPanel.setPartition(currentPartition);
            graphPanel.repaint();
            
            JOptionPane.showMessageDialog(this, 
                "Graph partitioned successfully\nCut edges: " + currentPartition.getCutEdges(),
                "Partition Complete", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error partitioning graph: " + ex.getMessage(),
                "Partition Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }
}