package pl.edu.graph.model;

public class Partition {
    private int[] assignments;
    private int[] partSizes;
    private int partCount;
    private int cutEdges;
    private int marginPercent;
    
    public Partition(int[] assignments, int[] partSizes, int partCount, int cutEdges, int marginPercent) {
        this.assignments = assignments;
        this.partSizes = partSizes;
        this.partCount = partCount;
        this.cutEdges = cutEdges;
        this.marginPercent = marginPercent;
    }
    
    public int getAssignment(int vertex) {
        if (vertex < 0 || vertex >= assignments.length) {
            throw new IllegalArgumentException("Invalid vertex index");
        }
        return assignments[vertex];
    }
    
    public int getPartSize(int part) {
        if (part < 0 || part >= partCount) {
            throw new IllegalArgumentException("Invalid part index");
        }
        return partSizes[part];
    }
    
    public int getPartCount() {
        return partCount;
    }
    
    public int getCutEdges() {
        return cutEdges;
    }
    
    public int getMarginPercent() {
        return marginPercent;
    }
}