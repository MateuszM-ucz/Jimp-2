package pl.edu.graph.ui;

import pl.edu.graph.model.Graph;
import pl.edu.graph.model.Partition;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GraphPanel extends JPanel {
    private Graph graph;
    private Partition partition;
    private Map<Integer, Point> vertexPositions;
    private static final int VERTEX_SIZE = 12;
    private static final Color[] PART_COLORS = {
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
        Color.CYAN, Color.PINK, Color.YELLOW, new Color(165, 42, 42),
        new Color(50, 205, 50), new Color(255, 20, 147)
    };
    
    public GraphPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEtchedBorder());
        vertexPositions = new HashMap<>();
    }
    
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.partition = null;  // Clear any existing partition
        calculatePositions();
        repaint();
    }
    
    public void setPartition(Partition partition) {
        this.partition = partition;
        repaint();
    }
    
    private void calculatePositions() {
        if (graph == null) return;
        
        vertexPositions.clear();
        int vertices = graph.getVertexCount();
        
        // Simple force-directed layout algorithm
        // For simplicity, we'll use a circular layout for now
        double radius = Math.min(getWidth(), getHeight()) * 0.4;
        if (radius < 100) radius = 200;  // Default if not yet sized
        
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        
        // Position vertices in a circle initially
        for (int i = 0; i < vertices; i++) {
            double angle = 2 * Math.PI * i / vertices;
            int x = (int) (centerX + radius * Math.cos(angle));
            int y = (int) (centerY + radius * Math.sin(angle));
            vertexPositions.put(i, new Point(x, y));
        }
        
        // Adjust size of panel based on graph size
        int requiredSize = (int) (radius * 2.2);
        setPreferredSize(new Dimension(requiredSize, requiredSize));
        revalidate();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (graph == null) {
            g.setColor(Color.GRAY);
            g.drawString("No graph loaded", getWidth()/2 - 40, getHeight()/2);
            return;
        }
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // If panel size changed, recalculate positions
        if (vertexPositions.isEmpty() || vertexPositions.size() != graph.getVertexCount()) {
            calculatePositions();
        }
        
        // Draw edges
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(1.0f));
        
        for (int u = 0; u < graph.getVertexCount(); u++) {
            Point p1 = vertexPositions.get(u);
            
            for (int v : graph.getNeighbors(u)) {
                // Only draw if u < v to avoid drawing twice
                if (u < v) {
                    Point p2 = vertexPositions.get(v);
                    if (p1 != null && p2 != null) {
                        g2.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
                    }
                }
            }
        }
        
        // Draw vertices
        for (int v = 0; v < graph.getVertexCount(); v++) {
            Point p = vertexPositions.get(v);
            if (p == null) continue;
            
            // Determine color based on partition
            if (partition != null) {
                int part = partition.getAssignment(v);
                int colorIndex = part % PART_COLORS.length;
                g2.setColor(PART_COLORS[colorIndex]);
            } else {
                g2.setColor(Color.GRAY);
            }
            
            g2.fill(new Ellipse2D.Double(p.x - VERTEX_SIZE/2.0, p.y - VERTEX_SIZE/2.0, 
                                       VERTEX_SIZE, VERTEX_SIZE));
            
            g2.setColor(Color.BLACK);
            g2.draw(new Ellipse2D.Double(p.x - VERTEX_SIZE/2.0, p.y - VERTEX_SIZE/2.0, 
                                       VERTEX_SIZE, VERTEX_SIZE));
            
            // Draw vertex number for smaller graphs
            if (graph.getVertexCount() <= 100) {
                g2.drawString(Integer.toString(v), p.x - 3, p.y + 4);
            }
        }
    }
}