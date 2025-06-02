package com.example.graphpartitioner.ui;

import javax.swing.*;

/**
 * Główna klasa aplikacji - punkt wejścia
 */
public class MainApplication {
    
    public static void main(String[] args) {
        // Ustaw Look and Feel systemu
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Użyj domyślnego Look and Feel jeśli systemowy nie jest dostępny
            e.printStackTrace();
        }
        
        // Uruchom aplikację w wątku Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}