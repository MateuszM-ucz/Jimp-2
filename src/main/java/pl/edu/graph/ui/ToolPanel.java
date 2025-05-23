package pl.edu.graph.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ToolPanel extends JPanel {
    private JSpinner partCountSpinner;
    private JSlider marginSlider;
    private JComboBox<String> algorithmComboBox;
    private JCheckBox hybridCheckbox;
    private JButton partitionButton;
    
    public ToolPanel() {
        setPreferredSize(new Dimension(250, getHeight()));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createTitledBorder("Partition Settings")
        ));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Number of parts
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Number of parts:"), gbc);
        
        gbc.gridx = 1;
        SpinnerNumberModel partModel = new SpinnerNumberModel(2, 2, 100, 1);
        partCountSpinner = new JSpinner(partModel);
        add(partCountSpinner, gbc);
        
        // Margin percentage
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Margin (%):"), gbc);
        
        gbc.gridx = 1;
        marginSlider = new JSlider(0, 100, 10);
        marginSlider.setMajorTickSpacing(20);
        marginSlider.setMinorTickSpacing(5);
        marginSlider.setPaintTicks(true);
        marginSlider.setPaintLabels(true);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy++;
        add(marginSlider, gbc);
        
        // Algorithm selection
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Algorithm:"), gbc);
        
        gbc.gridx = 1;
        algorithmComboBox = new JComboBox<>(new String[] {
            "modulo", "sekwencyjny", "losowy"
        });
        add(algorithmComboBox, gbc);
        
        // Hybrid checkbox
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        hybridCheckbox = new JCheckBox("Use hybrid algorithm");
        hybridCheckbox.addActionListener(e -> {
            algorithmComboBox.setEnabled(!hybridCheckbox.isSelected());
        });
        add(hybridCheckbox, gbc);
        
        // Partition button
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        partitionButton = new JButton("Partition Graph");
        partitionButton.setEnabled(false);
        add(partitionButton, gbc);
        
        // Add some spacing
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }
    
    public void setPartitionButtonAction(ActionListener action) {
        partitionButton.addActionListener(action);
    }
    
    public void setGraphLoaded(boolean loaded) {
        partitionButton.setEnabled(loaded);
    }
    
    public int getPartitionCount() {
        return (Integer) partCountSpinner.getValue();
    }
    
    public int getMarginPercent() {
        return marginSlider.getValue();
    }
    
    public String getSelectedAlgorithm() {
        return (String) algorithmComboBox.getSelectedItem();
    }
    
    public boolean isHybridSelected() {
        return hybridCheckbox.isSelected();
    }
}