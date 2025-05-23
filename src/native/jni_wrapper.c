#include "../../graf.h"
#include "../../podzial.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../../algorytm_kl.h"
#include "../../algorytm_hybrydowy.h"
#include "../../utils.h"

// Helper function to get Java String as C string
char* jstringToChar(JNIEnv* env, jstring jstr) {
    if (!jstr) return NULL;
    
    const char* utf = (*env)->GetStringUTFChars(env, jstr, NULL);
    if (!utf) return NULL;
    
    char* result = strdup(utf);
    (*env)->ReleaseStringUTFChars(env, jstr, utf);
    
    return result;
}

// Function to load a graph from a file
JNIEXPORT jobject JNICALL Java_pl_edu_graph_jni_GraphPartitionerNative_loadGraph
  (JNIEnv* env, jobject obj, jstring jfilePath) {
    char* filePath = jstringToChar(env, jfilePath);
    if (!filePath) {
        return NULL;
    }

    printf("Loading graph from file: %s\n", filePath);
    
    // Load the graph using the C function
    Graf* graf = wczytaj_graf(filePath);
    free(filePath);
    
    if (!graf) {
        printf("Failed to load graph\n");
        return NULL;
    }
    
    printf("Graph loaded successfully with %d vertices and %d edges\n", 
           graf->liczba_wierzcholkow, graf->liczba_krawedzi);
    
    // Create Java arrays for the graph structure
    jintArray jrowPointers = (*env)->NewIntArray(env, graf->liczba_wierzcholkow + 1);
    jintArray jadjacencyList = (*env)->NewIntArray(env, graf->wskazniki_listy[graf->liczba_wierzcholkow]);
    
    if (!jrowPointers || !jadjacencyList) {
        printf("Failed to create Java arrays\n");
        if (jrowPointers) (*env)->DeleteLocalRef(env, jrowPointers);
        if (jadjacencyList) (*env)->DeleteLocalRef(env, jadjacencyList);
        zwolnij_graf(graf);
        return NULL;
    }
    
    // Copy data to Java arrays
    (*env)->SetIntArrayRegion(env, jrowPointers, 0, graf->liczba_wierzcholkow + 1, graf->wskazniki_listy);
    (*env)->SetIntArrayRegion(env, jadjacencyList, 0, graf->wskazniki_listy[graf->liczba_wierzcholkow], 
                            graf->lista_sasiedztwa);
    
    // Find the Graph class and constructor
    jclass graphClass = (*env)->FindClass(env, "pl/edu/graph/model/Graph");
    if (!graphClass) {
        printf("Failed to find Graph class\n");
        (*env)->DeleteLocalRef(env, jrowPointers);
        (*env)->DeleteLocalRef(env, jadjacencyList);
        zwolnij_graf(graf);
        return NULL;
    }
    
    jmethodID constructor = (*env)->GetMethodID(env, graphClass, "<init>", 
                                             "(II[I[I)V");
    if (!constructor) {
        printf("Failed to find Graph constructor\n");
        (*env)->DeleteLocalRef(env, graphClass);
        (*env)->DeleteLocalRef(env, jrowPointers);
        (*env)->DeleteLocalRef(env, jadjacencyList);
        zwolnij_graf(graf);
        return NULL;
    }
    
    // Create and return the Graph object
    jobject graphObj = (*env)->NewObject(env, graphClass, constructor,
        (jint)graf->liczba_wierzcholkow,
        (jint)graf->liczba_krawedzi,
        jrowPointers,
        jadjacencyList);
    
    // Free the C graph structure (the Java object has its own copy of the data)
    zwolnij_graf(graf);
    
    // Clean up local references
    (*env)->DeleteLocalRef(env, graphClass);
    (*env)->DeleteLocalRef(env, jrowPointers);
    (*env)->DeleteLocalRef(env, jadjacencyList);
    
    return graphObj;
}

// Function to partition a graph
JNIEXPORT jobject JNICALL Java_pl_edu_graph_jni_GraphPartitionerNative_partitionGraph
  (JNIEnv* env, jobject obj, jobject jgraph, jint jpartCount, jint jmarginPercent, 
   jstring jalgorithm, jboolean juseHybrid) {
    
    // Extract the graph data from the Java object
    jclass graphClass = (*env)->GetObjectClass(env, jgraph);
    
    jmethodID getVertexCount = (*env)->GetMethodID(env, graphClass, "getVertexCount", "()I");
    jmethodID getEdgeCount = (*env)->GetMethodID(env, graphClass, "getEdgeCount", "()I");
    
    jint vertexCount = (*env)->CallIntMethod(env, jgraph, getVertexCount);
    jint edgeCount = (*env)->CallIntMethod(env, jgraph, getEdgeCount);
    
    // Get the internal arrays
    jfieldID rowPointersField = (*env)->GetFieldID(env, graphClass, "rowPointers", "[I");
    jfieldID adjacencyListField = (*env)->GetFieldID(env, graphClass, "adjacencyList", "[I");
    
    jobject rowPointersObj = (*env)->GetObjectField(env, jgraph, rowPointersField);
    jobject adjacencyListObj = (*env)->GetObjectField(env, jgraph, adjacencyListField);
    
    jintArray rowPointersArray = (jintArray)rowPointersObj;
    jintArray adjacencyListArray = (jintArray)adjacencyListObj;
    
    jint* rowPointers = (*env)->GetIntArrayElements(env, rowPointersArray, NULL);
    jint* adjacencyList = (*env)->GetIntArrayElements(env, adjacencyListArray, NULL);
    
    jint rowPointersLength = (*env)->GetArrayLength(env, rowPointersArray);
    jint adjacencyListLength = (*env)->GetArrayLength(env, adjacencyListArray);
    
    // Create a C graph from the Java data
    Graf* graf = (Graf*)malloc(sizeof(Graf));
    graf->liczba_wierzcholkow = vertexCount;
    graf->liczba_krawedzi = edgeCount;
    
    graf->wskazniki_listy = (int*)malloc(rowPointersLength * sizeof(int));
    graf->lista_sasiedztwa = (int*)malloc(adjacencyListLength * sizeof(int));
    
    for (int i = 0; i < rowPointersLength; i++) {
        graf->wskazniki_listy[i] = rowPointers[i];
    }
    
    for (int i = 0; i < adjacencyListLength; i++) {
        graf->lista_sasiedztwa[i] = adjacencyList[i];
    }
    
    // Release the Java arrays
    (*env)->ReleaseIntArrayElements(env, rowPointersArray, rowPointers, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, adjacencyListArray, adjacencyList, JNI_ABORT);
    
    // Get algorithm type
    char* algorithm = jstringToChar(env, jalgorithm);
    int useHybrid = (juseHybrid == JNI_TRUE);
    
    // Partition the graph
    Podzial* podzial = NULL;
    
    if (useHybrid) {
        podzial = znajdz_najlepszy_podzial(graf, (int)jpartCount, (int)jmarginPercent);
    } else {
        if (strcmp(algorithm, "modulo") == 0) {
            podzial = inicjalizuj_podzial_modulo(graf, (int)jpartCount, (int)jmarginPercent);
        } else if (strcmp(algorithm, "sekwencyjny") == 0) {
            podzial = inicjalizuj_podzial_sekwencyjny(graf, (int)jpartCount, (int)jmarginPercent);
        } else if (strcmp(algorithm, "losowy") == 0) {
            podzial = inicjalizuj_podzial_losowy(graf, (int)jpartCount, (int)jmarginPercent);
        } else {
            podzial = inicjalizuj_podzial_modulo(graf, (int)jpartCount, (int)jmarginPercent);
        }
        
        // Apply Kernighan-Lin optimization if we have a valid partition
        if (podzial) {
            algorytm_kernighan_lin(graf, podzial);
        }
    }
    
    free(algorithm);
    
    if (!podzial) {
        zwolnij_graf(graf);
        return NULL;
    }
    
    // Create Java arrays for partition data
    jintArray jassignments = (*env)->NewIntArray(env, vertexCount);
    jintArray jpartSizes = (*env)->NewIntArray(env, podzial->liczba_czesci);
    
    // Copy data to Java arrays
    (*env)->SetIntArrayRegion(env, jassignments, 0, vertexCount, podzial->przypisania);
    (*env)->SetIntArrayRegion(env, jpartSizes, 0, podzial->liczba_czesci, podzial->rozmiary_czesci);
    
    // Find the Partition class and constructor
    jclass partitionClass = (*env)->FindClass(env, "pl/edu/graph/model/Partition");
    jmethodID constructor = (*env)->GetMethodID(env, partitionClass, "<init>", 
                                             "([I[IIII)V");
    
    // Create and return the Partition object
    jobject partitionObj = (*env)->NewObject(env, partitionClass, constructor,
        jassignments,
        jpartSizes,
        (jint)podzial->liczba_czesci,
        (jint)podzial->przeciete_krawedzie,
        (jint)podzial->margines_procentowy);
    
    // Free the C structures
    zwolnij_podzial(podzial);
    zwolnij_graf(graf);
    
    // Clean up local references
    (*env)->DeleteLocalRef(env, jassignments);
    (*env)->DeleteLocalRef(env, jpartSizes);
    (*env)->DeleteLocalRef(env, partitionClass);
    
    return partitionObj;
}

// Function to save a graph partition to a file
JNIEXPORT void JNICALL Java_pl_edu_graph_jni_GraphPartitionerNative_savePartition
  (JNIEnv* env, jobject obj, jstring jfilePath, jobject jgraph, jobject jpartition, jstring jformat) {
    
    char* filePath = jstringToChar(env, jfilePath);
    char* format = jstringToChar(env, jformat);
    
    if (!filePath || !format) {
        if (filePath) free(filePath);
        if (format) free(format);
        return;
    }
    
    // Extract the graph data
    jclass graphClass = (*env)->GetObjectClass(env, jgraph);
    jmethodID getVertexCount = (*env)->GetMethodID(env, graphClass, "getVertexCount", "()I");
    jmethodID getEdgeCount = (*env)->GetMethodID(env, graphClass, "getEdgeCount", "()I");
    
    jint vertexCount = (*env)->CallIntMethod(env, jgraph, getVertexCount);
    jint edgeCount = (*env)->CallIntMethod(env, jgraph, getEdgeCount);
    
    // Get the internal arrays
    jfieldID rowPointersField = (*env)->GetFieldID(env, graphClass, "rowPointers", "[I");
    jfieldID adjacencyListField = (*env)->GetFieldID(env, graphClass, "adjacencyList", "[I");
    
    jobject rowPointersObj = (*env)->GetObjectField(env, jgraph, rowPointersField);
    jobject adjacencyListObj = (*env)->GetObjectField(env, jgraph, adjacencyListField);
    
    jintArray rowPointersArray = (jintArray)rowPointersObj;
    jintArray adjacencyListArray = (jintArray)adjacencyListObj;
    
    jint* rowPointers = (*env)->GetIntArrayElements(env, rowPointersArray, NULL);
    jint* adjacencyList = (*env)->GetIntArrayElements(env, adjacencyListArray, NULL);
    
    jint rowPointersLength = (*env)->GetArrayLength(env, rowPointersArray);
    jint adjacencyListLength = (*env)->GetArrayLength(env, adjacencyListArray);
    
    // Create C graph
    Graf* graf = (Graf*)malloc(sizeof(Graf));
    graf->liczba_wierzcholkow = vertexCount;
    graf->liczba_krawedzi = edgeCount;
    
    graf->wskazniki_listy = (int*)malloc(rowPointersLength * sizeof(int));
    graf->lista_sasiedztwa = (int*)malloc(adjacencyListLength * sizeof(int));
    
    for (int i = 0; i < rowPointersLength; i++) {
        graf->wskazniki_listy[i] = rowPointers[i];
    }
    
    for (int i = 0; i < adjacencyListLength; i++) {
        graf->lista_sasiedztwa[i] = adjacencyList[i];
    }
    
    // Release Java arrays
    (*env)->ReleaseIntArrayElements(env, rowPointersArray, rowPointers, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, adjacencyListArray, adjacencyList, JNI_ABORT);
    
    // Extract partition data
    jclass partitionClass = (*env)->GetObjectClass(env, jpartition);
    
    jmethodID getPartCount = (*env)->GetMethodID(env, partitionClass, "getPartCount", "()I");
    jmethodID getCutEdges = (*env)->GetMethodID(env, partitionClass, "getCutEdges", "()I");
    jmethodID getMarginPercent = (*env)->GetMethodID(env, partitionClass, "getMarginPercent", "()I");
    
    jint partCount = (*env)->CallIntMethod(env, jpartition, getPartCount);
    jint cutEdges = (*env)->CallIntMethod(env, jpartition, getCutEdges);
    jint marginPercent = (*env)->CallIntMethod(env, jpartition, getMarginPercent);
    
    // Get partition assignments and sizes
    jfieldID assignmentsField = (*env)->GetFieldID(env, partitionClass, "assignments", "[I");
    jfieldID partSizesField = (*env)->GetFieldID(env, partitionClass, "partSizes", "[I");
    
    jobject assignmentsObj = (*env)->GetObjectField(env, jpartition, assignmentsField);
    jobject partSizesObj = (*env)->GetObjectField(env, jpartition, partSizesField);
    
    jintArray assignmentsArray = (jintArray)assignmentsObj;
    jintArray partSizesArray = (jintArray)partSizesObj;
    
    jint* assignments = (*env)->GetIntArrayElements(env, assignmentsArray, NULL);
    jint* partSizes = (*env)->GetIntArrayElements(env, partSizesArray, NULL);
    
    // Create C partition
    Podzial* podzial = (Podzial*)malloc(sizeof(Podzial));
    podzial->liczba_czesci = partCount;
    podzial->przeciete_krawedzie = cutEdges;
    podzial->margines_procentowy = marginPercent;
    
    podzial->przypisania = (int*)malloc(vertexCount * sizeof(int));
    podzial->rozmiary_czesci = (int*)malloc(partCount * sizeof(int));
    
    for (int i = 0; i < vertexCount; i++) {
        podzial->przypisania[i] = assignments[i];
    }
    
    for (int i = 0; i < partCount; i++) {
        podzial->rozmiary_czesci[i] = partSizes[i];
    }
    
    // Release Java arrays
    (*env)->ReleaseIntArrayElements(env, assignmentsArray, assignments, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, partSizesArray, partSizes, JNI_ABORT);
    
    // Save the partition
    printf("Saving partition to file: %s with format: %s\n", filePath, format);
    zapisz_podzial(filePath, graf, podzial, format);
    
    // Free resources
    zwolnij_podzial(podzial);
    zwolnij_graf(graf);
    free(filePath);
    free(format);
}