
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Vehicle Rental Management System");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel title = new JLabel(
                "VEHICLE RENTAL MANAGEMENT SYSTEM",
                SwingConstants.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton vehicleButton = new JButton("Vehicle Management");
        JButton customerButton = new JButton("Customer Management");
        JButton rentalButton = new JButton("Rent Vehicle");
        JButton returnButton = new JButton("Return Vehicle");
        JButton historyButton = new JButton("Rental History");
        JButton exitButton = new JButton("Exit");

        Font buttonFont = new Font("Arial", Font.PLAIN, 18);

        vehicleButton.setFont(buttonFont);
        customerButton.setFont(buttonFont);
        rentalButton.setFont(buttonFont);
        returnButton.setFont(buttonFont);
        historyButton.setFont(buttonFont);
        exitButton.setFont(buttonFont);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        buttonPanel.setBorder(
                BorderFactory.createEmptyBorder(30, 50, 30, 50)
        );

        buttonPanel.add(vehicleButton);
        buttonPanel.add(customerButton);
        buttonPanel.add(rentalButton);
        buttonPanel.add(returnButton);
        buttonPanel.add(historyButton);
        buttonPanel.add(exitButton);

        setLayout(new BorderLayout());
        add(title, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);

        vehicleButton.addActionListener(e ->
                new VehicleManagement().setVisible(true));

        customerButton.addActionListener(e ->
                new CustomerManagement().setVisible(true));

        rentalButton.addActionListener(e ->
                new RentalManagement().setVisible(true));

        returnButton.addActionListener(e ->
                new ReturnVehicle().setVisible(true));

        historyButton.addActionListener(e ->
                new RentalHistory().setVisible(true));

        exitButton.addActionListener(e -> System.exit(0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
