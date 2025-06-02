package com.example.graphpartitioner.ui;

import com.example.graphpartitioner.model.Graph;
import com.example.graphpartitioner.model.Partition;
import com.example.graphpartitioner.model.Point2DDouble;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Panel do wizualizacji grafu z możliwością interakcji
 */
public class GraphPanel extends JPanel {
    private Graph graph;
    private Partition partition;
    private Map<Integer, Point2DDouble> vertexPositions;
    
    // Parametry widoku
    private double scale = 1.0;
    private double panX = 0;
    private double panY = 0;
    
    // Stan interakcji
    private Point dragStartPoint;
    private Integer draggedVertex;
    private boolean isPanning = false;
    
    // Kolory dla części
    private static final Color[] PART_COLORS = {
        new Color(255, 99, 71),   // Tomato
        new Color(30, 144, 255),  // DodgerBlue
        new Color(50, 205, 50),   // LimeGreen
        new Color(255, 215, 0),   // Gold
        new Color(148, 0, 211),   // DarkViolet
        new Color(255, 140, 0),   // DarkOrange
        new Color(0, 206, 209),   // DarkTurquoise
        new Color(255, 20, 147),  // DeepPink
        new Color(70, 130, 180),  // SteelBlue
        new Color(154, 205, 50)   // YellowGreen
    };
    
    // Parametry rysowania
    private static final int VERTEX_RADIUS = 20;
    private static final int FONT_SIZE = 12;
    private static final Stroke NORMAL_STROKE = new BasicStroke(1.0f);
    private static final Stroke THICK_STROKE = new BasicStroke(2.0f);
    private static final Stroke CUT_EDGE_STROKE = new BasicStroke(2.0f, 
        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5, 5}, 0);
    
    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
        
        initializeMouseListeners();
    }
    
    /**
     * Ustawia graf i podział do wyświetlenia
     */
    public void setGraph(Graph graph, Partition partition) {
        this.graph = graph;
        this.partition = partition;
        
        if (graph != null) {
            calculateVertexPositions();
        }
        
        repaint();
    }
    
    /**
     * Oblicza pozycje wierzchołków (układ kołowy)
     */
    private void calculateVertexPositions() {
        vertexPositions = new HashMap<>();
        
        if (graph == null || graph.getVertexCount() == 0) {
            return;
        }
        
        int n = graph.getVertexCount();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        
        // Promień zależy od liczby wierzchołków
        double radius = Math.min(getWidth(), getHeight()) * 0.35;
        
        // Grupuj wierzchołki według części (jeśli istnieje podział)
        if (partition != null && partition.getPartCount() > 1) {
            // Układamy wierzchołki w grupach według części
            Map<Integer, List<Integer>> verticesByPart = new HashMap<>();
            for (int i = 0; i < partition.getPartCount(); i++) {
                verticesByPart.put(i, new ArrayList<>());
            }
            
            for (int v = 0; v < n; v++) {
                int part = partition.getAssignment(v);
                if (part >= 0 && part < partition.getPartCount()) {
                    verticesByPart.get(part).add(v);
                }
            }
            
            // Układamy każdą część w sektorze koła
            double anglePerPart = 2 * Math.PI / partition.getPartCount();
            
            for (int part = 0; part < partition.getPartCount(); part++) {
                List<Integer> vertices = verticesByPart.get(part);
                if (vertices.isEmpty()) continue;
                
                double startAngle = part * anglePerPart;
                double endAngle = (part + 1) * anglePerPart;
                double angleStep = (endAngle - startAngle) / vertices.size();
                
                for (int i = 0; i < vertices.size(); i++) {
                    double angle = startAngle + i * angleStep + angleStep / 2;
                    double x = centerX + radius * Math.cos(angle);
                    double y = centerY + radius * Math.sin(angle);
                    vertexPositions.put(vertices.get(i), new Point2DDouble(x, y));
                }
            }
        } else {
            // Zwykły układ kołowy
            double angleStep = 2 * Math.PI / n;
            
            for (int i = 0; i < n; i++) {
                double angle = i * angleStep;
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                vertexPositions.put(i, new Point2DDouble(x, y));
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (graph == null || vertexPositions == null) {
            // Wyświetl komunikat jeśli brak grafu
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            String msg = "Wczytaj graf z menu Plik";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = getHeight() / 2;
            g.drawString(msg, x, y);
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Zapisz oryginalną transformację
        AffineTransform originalTransform = g2d.getTransform();
        
        // Zastosuj transformacje dla pan i zoom
        g2d.translate(panX, panY);
        g2d.scale(scale, scale);
        
        // Rysuj krawędzie
        drawEdges(g2d);
        
        // Rysuj wierzchołki
        drawVertices(g2d);
        
        // Przywróć oryginalną transformację
        g2d.setTransform(originalTransform);
        
        // Rysuj informacje o grafie
        drawGraphInfo(g2d);
    }
    
    /**
     * Rysuje krawędzie grafu
     */
    private void drawEdges(Graphics2D g2d) {
        Set<String> drawnEdges = new HashSet<>();
        
        for (int u = 0; u < graph.getVertexCount(); u++) {
            Point2DDouble posU = vertexPositions.get(u);
            if (posU == null) continue;
            
            List<Integer> neighbors = graph.getNeighbors(u);
            for (int v : neighbors) {
                if (u < v) { // Rysuj każdą krawędź tylko raz
                    Point2DDouble posV = vertexPositions.get(v);
                    if (posV == null) continue;
                    
                    // Sprawdź czy krawędź jest przecięta
                    boolean isCutEdge = false;
                    if (partition != null) {
                        int partU = partition.getAssignment(u);
                        int partV = partition.getAssignment(v);
                        isCutEdge = (partU != partV);
                    }
                    
                    // Ustaw styl linii
                    if (isCutEdge) {
                        g2d.setColor(Color.RED);
                        g2d.setStroke(CUT_EDGE_STROKE);
                    } else {
                        g2d.setColor(Color.GRAY);
                        g2d.setStroke(NORMAL_STROKE);
                    }
                    
                    // Rysuj krawędź
                    g2d.drawLine((int)posU.x, (int)posU.y, (int)posV.x, (int)posV.y);
                }
            }
        }
    }
    
    /**
     * Rysuje wierzchołki grafu
     */
    private void drawVertices(Graphics2D g2d) {
        g2d.setStroke(THICK_STROKE);
        g2d.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
        
        for (int v = 0; v < graph.getVertexCount(); v++) {
            Point2DDouble pos = vertexPositions.get(v);
            if (pos == null) continue;
            
            // Wybierz kolor na podstawie części
            Color vertexColor = Color.LIGHT_GRAY;
            if (partition != null) {
                int part = partition.getAssignment(v);
                if (part >= 0 && part < PART_COLORS.length) {
                    vertexColor = PART_COLORS[part];
                } else if (part >= 0) {
                    // Generuj kolor dla części > 10
                    vertexColor = generateColorForPart(part);
                }
            }
            
            // Rysuj wypełniony okrąg
            g2d.setColor(vertexColor);
            g2d.fillOval((int)(pos.x - VERTEX_RADIUS), (int)(pos.y - VERTEX_RADIUS), 
                        2 * VERTEX_RADIUS, 2 * VERTEX_RADIUS);
            
            // Rysuj obramowanie
            g2d.setColor(Color.BLACK);
            g2d.drawOval((int)(pos.x - VERTEX_RADIUS), (int)(pos.y - VERTEX_RADIUS), 
                        2 * VERTEX_RADIUS, 2 * VERTEX_RADIUS);
            
            // Rysuj numer wierzchołka
            String label = String.valueOf(v);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            int labelHeight = fm.getHeight();
            g2d.drawString(label, (int)(pos.x - labelWidth/2), (int)(pos.y + labelHeight/4));
        }
    }
    
    /**
     * Rysuje informacje o grafie
     */
    private void drawGraphInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        
        int y = 20;
        g2d.drawString("Wierzchołki: " + graph.getVertexCount(), 10, y);
        y += 15;
        g2d.drawString("Krawędzie: " + graph.getEdgeCount(), 10, y);
        
        if (partition != null) {
            y += 15;
            g2d.drawString("Części: " + partition.getPartCount(), 10, y);
            y += 15;
            g2d.drawString("Przecięte krawędzie: " + partition.getCutEdges(), 10, y);
        }
        
        y += 15;
        g2d.drawString(String.format("Zoom: %.0f%%", scale * 100), 10, y);
    }
    
    /**
     * Generuje kolor dla części o indeksie >= 10
     */
    private Color generateColorForPart(int part) {
        Random rand = new Random(part); // Seed dla spójności kolorów
        float hue = rand.nextFloat();
        float saturation = 0.5f + rand.nextFloat() * 0.5f;
        float brightness = 0.7f + rand.nextFloat() * 0.3f;
        return Color.getHSBColor(hue, saturation, brightness);
    }
    
    /**
     * Inicjalizuje obsługę myszy
     */
    private void initializeMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMoved(e);
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        
        // Obsługa kółka myszy dla zoom
        addMouseWheelListener(e -> handleMouseWheel(e));
    }
    
    private void handleMousePressed(MouseEvent e) {
        dragStartPoint = e.getPoint();
        
        if (e.getButton() == MouseEvent.BUTTON1) {
            // Lewy przycisk - sprawdź czy kliknięto na wierzchołek
            draggedVertex = getVertexAt(e.getPoint());
            if (draggedVertex == null) {
                // Jeśli nie kliknięto na wierzchołek, rozpocznij pan
                isPanning = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // Prawy przycisk - rozpocznij pan
            isPanning = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        draggedVertex = null;
        isPanning = false;
        setCursor(Cursor.getDefaultCursor());
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (draggedVertex != null && vertexPositions != null) {
            // Przeciąganie wierzchołka
            Point2DDouble worldPos = screenToWorld(e.getPoint());
            vertexPositions.put(draggedVertex, worldPos);
            repaint();
        } else if (isPanning && dragStartPoint != null) {
            // Pan widoku
            double dx = e.getX() - dragStartPoint.x;
            double dy = e.getY() - dragStartPoint.y;
            panX += dx;
            panY += dy;
            dragStartPoint = e.getPoint();
            repaint();
        }
    }
    
    private void handleMouseMoved(MouseEvent e) {
        // Zmień kursor gdy nad wierzchołkiem
        Integer vertex = getVertexAt(e.getPoint());
        if (vertex != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    private void handleMouseWheel(MouseWheelEvent e) {
        double oldScale = scale;
        double scaleFactor = 1.1;
        
        if (e.getWheelRotation() < 0) {
            scale *= scaleFactor;
        } else {
            scale /= scaleFactor;
        }
        
        // Ogranicz zoom
        scale = Math.max(0.1, Math.min(10.0, scale));
        
        // Zoom względem pozycji kursora
        if (scale != oldScale) {
            Point mousePos = e.getPoint();
            double dx = mousePos.x - panX;
            double dy = mousePos.y - panY;
            
            panX = mousePos.x - dx * scale / oldScale;
            panY = mousePos.y - dy * scale / oldScale;
            
            repaint();
        }
    }
    
    /**
     * Znajduje wierzchołek w danym punkcie ekranu
     */
    private Integer getVertexAt(Point screenPoint) {
        if (graph == null || vertexPositions == null) {
            return null;
        }
        
        Point2DDouble worldPoint = screenToWorld(screenPoint);
        
        for (Map.Entry<Integer, Point2DDouble> entry : vertexPositions.entrySet()) {
            Point2DDouble pos = entry.getValue();
            double distance = pos.distanceTo(worldPoint.x, worldPoint.y);
            if (distance <= VERTEX_RADIUS) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Konwertuje współrzędne ekranowe na współrzędne świata
     */
    private Point2DDouble screenToWorld(Point screenPoint) {
        double x = (screenPoint.x - panX) / scale;
        double y = (screenPoint.y - panY) / scale;
        return new Point2DDouble(x, y);
    }
    
    /**
     * Resetuje widok do początkowego
     */
    public void resetView() {
        scale = 1.0;
        panX = 0;
        panY = 0;
        if (graph != null) {
            calculateVertexPositions();
        }
        repaint();
    }
    
    /**
     * Dopasowuje widok do okna
     */
    public void fitToWindow() {
        if (graph == null || vertexPositions == null || vertexPositions.isEmpty()) {
            return;
        }
        
        // Znajdź granice
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Point2DDouble pos : vertexPositions.values()) {
            minX = Math.min(minX, pos.x - VERTEX_RADIUS);
            minY = Math.min(minY, pos.y - VERTEX_RADIUS);
            maxX = Math.max(maxX, pos.x + VERTEX_RADIUS);
            maxY = Math.max(maxY, pos.y + VERTEX_RADIUS);
        }
        
        double graphWidth = maxX - minX;
        double graphHeight = maxY - minY;
        
        if (graphWidth > 0 && graphHeight > 0) {
            double scaleX = (getWidth() - 40) / graphWidth;
            double scaleY = (getHeight() - 40) / graphHeight;
            scale = Math.min(scaleX, scaleY);
            
            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;
            
            panX = getWidth() / 2 - centerX * scale;
            panY = getHeight() / 2 - centerY * scale;
            
            repaint();
        }
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (graph != null && vertexPositions != null && vertexPositions.isEmpty()) {
            calculateVertexPositions();
        }
    }
}