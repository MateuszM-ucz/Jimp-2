package pl.edu.graph.ui;

import pl.edu.graph.model.Graph;
import pl.edu.graph.model.Partition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;

public class GraphPanel extends JPanel {
    private Graph graph;
    private Partition partition;
    private double[] nodeX; // X coordinates for nodes
    private double[] nodeY; // Y coordinates for nodes
    private boolean layoutInitialized = false;
    private int iterations = 100; // How many iterations of force-directed algorithm to run
    
    // Force-directed layout parameters
    private double k = 0.01; // Spring constant
    private double repulsion = 500.0; // Repulsion force
    private double timeStep = 0.1; // Time step for simulation
    private double damping = 0.95; // Damping factor to stabilize
    
    // For zooming and panning
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private Point lastMousePos;
    
    // Add these fields if they don't exist
    private double zoomFactor = 1.0;
    private static final double ZOOM_CHANGE_FACTOR = 0.1;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private Point zoomPoint = new Point(0, 0);
    private int nodeRadius = 20; // Base node size
    
    // Add these variables for partitioning
    private boolean partitioned = false;
    private boolean[] partitionAssignment; // true = red partition, false = green partition
    
    public GraphPanel() {
        setBackground(Color.WHITE);
        
        // Add mouse listeners for panning and zooming
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePos != null) {
                    // Calculate the drag delta
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;
                    
                    // Update the translation values, adjusted for zoom
                    translateX += dx / zoomFactor;
                    translateY += dy / zoomFactor;
                    
                    // Update last position
                    lastMousePos = e.getPoint();
                    
                    // Repaint with the new translation
                    repaint();
                }
            }
            
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                double oldZoom = zoomFactor;
                
                if (rotation < 0) {
                    // Zoom in
                    zoomFactor = Math.min(zoomFactor + ZOOM_CHANGE_FACTOR, MAX_ZOOM);
                } else {
                    // Zoom out
                    zoomFactor = Math.max(zoomFactor - ZOOM_CHANGE_FACTOR, MIN_ZOOM);
                }
                
                // Set the zoom center to the mouse position
                zoomPoint = e.getPoint();
                
                // Adjust the node radius based on zoom (but not as dramatically)
                nodeRadius = (int)(15 * Math.sqrt(zoomFactor));
                
                repaint();
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
        
        // Add component listener for resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (graph != null && layoutInitialized) {
                    // Recalculate layout when window size changes
                    runForceDirectedLayout();
                    repaint();
                }
            }
        });
    }
    
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.partition = null;
        this.layoutInitialized = false;
        
        if (graph != null) {
            initializeForceDirectedLayout();
            runForceDirectedLayout();
        }
        
        repaint();
    }
    
    public void setPartition(Partition partition) {
        this.partition = partition;
        repaint();
    }
    
    private void initializeForceDirectedLayout() {
        if (graph == null) return;
        
        int n = graph.getVertexCount();
        nodeX = new double[n];
        nodeY = new double[n];
        
        // Initialize nodes in a more scattered random position rather than just a circle
        Random random = new Random(42); // Fixed seed for consistent initial layout
        double width = Math.max(800, getWidth());
        double height = Math.max(600, getHeight());
        
        for (int i = 0; i < n; i++) {
            // Use more of the available space for initial positions
            nodeX[i] = random.nextDouble() * width * 0.8 + width * 0.1;
            nodeY[i] = random.nextDouble() * height * 0.8 + height * 0.1;
        }
        
        layoutInitialized = true;
    }
    
    private void runForceDirectedLayout() {
        if (!layoutInitialized || graph == null) return;
        
        int n = graph.getVertexCount();
        double[] forceX = new double[n];
        double[] forceY = new double[n];
        
        // Adjust parameters based on graph size
        double optimalDistance = Math.sqrt(getWidth() * getHeight() / (double)n) * 1.5;
        k = 0.2; // Stronger spring constant
        repulsion = optimalDistance * 50; // Scale repulsion with graph size
        iterations = Math.min(200, Math.max(100, n / 2)); // More iterations for larger graphs
        
        // Run more iterations with cooling
        double initialTimeStep = timeStep;
        for (int iter = 0; iter < iterations; iter++) {
            // Cooling: reduce time step gradually
            timeStep = initialTimeStep * (1 - (double)iter / iterations);
            
            // Reset forces
            for (int i = 0; i < n; i++) {
                forceX[i] = 0;
                forceY[i] = 0;
            }
            
            // Apply repulsive forces between all nodes
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        double dx = nodeX[i] - nodeX[j];
                        double dy = nodeY[i] - nodeY[j];
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        
                        // Prevent division by zero
                        if (distance < 0.1) {
                            distance = 0.1;
                            // Add slight jitter to prevent nodes from stacking
                            dx = 0.1 + Math.random() * 0.1;
                            dy = 0.1 + Math.random() * 0.1;
                        }
                        
                        // Repulsive force is inversely proportional to distance
                        double force = repulsion / (distance * distance);
                        
                        // Add repulsive force in vector components
                        forceX[i] += dx / distance * force;
                        forceY[i] += dy / distance * force;
                    }
                }
            }
            
            // Apply attractive forces between connected nodes
            int[] rowPointers = graph.getRowPointers();
            int[] adjacencyList = graph.getAdjacencyList();
            
            for (int i = 0; i < n; i++) {
                int start = rowPointers[i];
                int end = rowPointers[i + 1];
                
                for (int j = start; j < end; j++) {
                    int neighbor = adjacencyList[j];
                    
                    // Apply force in both directions
                    double dx = nodeX[i] - nodeX[neighbor];
                    double dy = nodeY[i] - nodeY[neighbor];
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    
                    if (distance < 0.1) distance = 0.1;
                    
                    // Spring force proportional to distance
                    double force = k * (distance - optimalDistance);
                    
                    forceX[i] -= dx / distance * force;
                    forceY[i] -= dy / distance * force;
                    forceX[neighbor] += dx / distance * force;
                    forceY[neighbor] += dy / distance * force;
                }
            }
            
            // Update positions based on forces
            for (int i = 0; i < n; i++) {
                // Apply damping to stabilize
                nodeX[i] += forceX[i] * timeStep * damping;
                nodeY[i] += forceY[i] * timeStep * damping;
            }
            
            // Apply boundaries to keep nodes in view
            for (int i = 0; i < n; i++) {
                nodeX[i] = Math.max(50, Math.min(getWidth() - 50, nodeX[i]));
                nodeY[i] = Math.max(50, Math.min(getHeight() - 50, nodeY[i]));
            }
        }
        
        // Reset timeStep for next run
        timeStep = initialTimeStep;
        
        // Center the graph in the panel
        centerGraph();
    }
    
    private void centerGraph() {
        if (!layoutInitialized || graph == null) return;
        
        int n = graph.getVertexCount();
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        
        for (int i = 0; i < n; i++) {
            minX = Math.min(minX, nodeX[i]);
            maxX = Math.max(maxX, nodeX[i]);
            minY = Math.min(minY, nodeY[i]);
            maxY = Math.max(maxY, nodeY[i]);
        }
        
        double centerX = (maxX + minX) / 2;
        double centerY = (maxY + minY) / 2;
        double panelCenterX = getWidth() / 2;
        double panelCenterY = getHeight() / 2;
        
        double offsetX = panelCenterX - centerX;
        double offsetY = panelCenterY - centerY;
        
        for (int i = 0; i < n; i++) {
            nodeX[i] += offsetX;
            nodeY[i] += offsetY;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (graph == null) return;
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Save the original transform
        AffineTransform originalTransform = g2d.getTransform();
        
        // Apply zoom and pan transform
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        g2d.translate(centerX, centerY);
        g2d.scale(zoomFactor, zoomFactor);
        g2d.translate(-centerX, -centerY);
        
        // Apply panning translation
        g2d.translate(translateX, translateY);
        
        // Now draw everything with the zoom and pan applied
        
        // Calculate the actual node radius to use - compensate for scaling
        // This keeps nodes a consistent size regardless of zoom
        int displayRadius = (int)(nodeRadius / zoomFactor);
        
        // First draw edges
        g2d.setStroke(new BasicStroke(1.0f));
        
        for (int i = 0; i < graph.getVertexCount(); i++) {
            int startIdx = graph.getRowPointers()[i];
            int endIdx = graph.getRowPointers()[i + 1];
            
            int x1 = (int)nodeX[i];
            int y1 = (int)nodeY[i];
            
            for (int j = startIdx; j < endIdx; j++) {
                int neighbor = graph.getAdjacencyList()[j];
                
                // Only draw edge once (when i < neighbor)
                if (i < neighbor) {
                    int x2 = (int)nodeX[neighbor];
                    int y2 = (int)nodeY[neighbor];
                    
                    // Color the edge based on partitions (if partitions exist)
                    if (partitionAssignment != null) {
                        // Color edge red if either endpoint is in the red partition
                        if (partitionAssignment[i] || partitionAssignment[neighbor]) {
                            g2d.setColor(Color.RED);
                        } else {
                            g2d.setColor(Color.GREEN);
                        }
                    } else {
                        g2d.setColor(Color.GRAY);
                    }
                    
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
        
        // Then draw nodes
        for (int i = 0; i < graph.getVertexCount(); i++) {
            int x = (int)nodeX[i];
            int y = (int)nodeY[i];
            
            // Set color based on partition
            if (partitionAssignment != null) {
                g2d.setColor(partitionAssignment[i] ? Color.RED : Color.GREEN);
            } else {
                g2d.setColor(Color.BLUE);
            }
            
            g2d.fillOval(x - displayRadius, y - displayRadius, displayRadius * 2, displayRadius * 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - displayRadius, y - displayRadius, displayRadius * 2, displayRadius * 2);
            
            // Add node number
            String nodeLabel = String.valueOf(i);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(nodeLabel);
            int textHeight = fm.getHeight();
            
            // Draw the node number in white with black outline for better visibility
            g2d.setColor(Color.WHITE);
            g2d.drawString(nodeLabel, x - textWidth/2, y + textHeight/4);
        }
        
        // Restore original transform
        g2d.setTransform(originalTransform);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }
    
    // Add this method to your class
    public void partitionGraph() {
        if (graph == null) return;
        
        int n = graph.getVertexCount();
        partitionAssignment = new boolean[n];
        
        // Initialize all nodes as not visited (green partition)
        Arrays.fill(partitionAssignment, false);
        
        // Use BFS to color connected components
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();
        
        // Start with the first node
        queue.add(0);
        partitionAssignment[0] = true; // Red partition
        visited[0] = true;
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            
            // Get neighbors
            int start = graph.getRowPointers()[current];
            int end = graph.getRowPointers()[current + 1];
            
            for (int i = start; i < end; i++) {
                int neighbor = graph.getAdjacencyList()[i];
                
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    
                    // Color alternating (opposite of current node's color)
                    partitionAssignment[neighbor] = !partitionAssignment[current];
                    queue.add(neighbor);
                }
            }
        }
        
        // Make sure we visited all nodes (for disconnected graphs)
        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                // Start a new BFS from this unvisited node
                queue.add(i);
                partitionAssignment[i] = true; // Red partition
                visited[i] = true;
                
                while (!queue.isEmpty()) {
                    int current = queue.poll();
                    
                    int start = graph.getRowPointers()[current];
                    int end = graph.getRowPointers()[current + 1];
                    
                    for (int j = start; j < end; j++) {
                        int neighbor = graph.getAdjacencyList()[j];
                        
                        if (!visited[neighbor]) {
                            visited[neighbor] = true;
                            partitionAssignment[neighbor] = !partitionAssignment[current];
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        repaint();
    }
    
    // Update the resetPartitioning method
    public void resetPartitioning() {
        partitioned = false;
        partitionAssignment = null;
        repaint();
    }
}