package com.example.graphpartitioner.utils;

/**
 * Pomocnicza klasa generyczna do przechowywania par wartości
 */
public class Pair<U, V> {
    private final U first;
    private final V second;
    
    public Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }
    
    public U getFirst() {
        return first;
    }
    
    public V getSecond() {
        return second;
    }
    
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Pair<?, ?> pair = (Pair<?, ?>) o;
        
        if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
        return second != null ? second.equals(pair.second) : pair.second == null;
    }
    
    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}