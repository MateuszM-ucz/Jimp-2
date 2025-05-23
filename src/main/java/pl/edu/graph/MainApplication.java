package pl.edu.graph;

import pl.edu.graph.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainApplication {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}