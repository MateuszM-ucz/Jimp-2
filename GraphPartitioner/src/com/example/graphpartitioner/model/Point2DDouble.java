package com.example.graphpartitioner.model;

/**
 * Prosta klasa do przechowywania współrzędnych 2D
 */
public class Point2DDouble {
    public double x;
    public double y;
    
    public Point2DDouble(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Oblicza odległość do innego punktu
     */
    public double distanceTo(Point2DDouble other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Oblicza odległość do punktu (x, y)
     */
    public double distanceTo(double px, double py) {
        double dx = x - px;
        double dy = y - py;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}