package pl.edu.graph.jni;

import pl.edu.graph.model.Graph;
import pl.edu.graph.model.Partition;

public class GraphPartitionerNative {
    static {
        try {
            // Change this line to include the platform-specific folder
            NativeUtils.loadLibraryFromJar("/native/win32-x86-64/libgraphpartitioner");
        } catch (Exception e) {
            System.err.println("Failed to load native library: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public native Graph loadGraph(String filePath);
    
    public native Partition partitionGraph(Graph graph, int partCount, int marginPercent, 
                                          String algorithm, boolean useHybrid);
    
    public native void savePartition(String filePath, Graph graph, Partition partition, 
                                    String format);
}