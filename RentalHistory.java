
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RentalHistory extends JFrame {

    private JTable rentalTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public RentalHistory() {
        setTitle("Rental History");
        setSize(1150, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadRentalHistory("");
    }

    private void createInterface() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("RENTAL HISTORY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        // Search panel
        searchField = new JTextField();

        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBorder(
                BorderFactory.createTitledBorder("Search Rental")
        );

        searchPanel.add(new JLabel("Customer / Vehicle:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel();
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        add(searchPanel, BorderLayout.SOUTH);

        // Rental history table
        String[] columns = {
            "Rental ID", "Customer", "Vehicle", "Rental Date",
            "Expected Return", "Actual Return", "Rental Amount",
            "Fine", "Total Amount", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        rentalTable = new JTable(tableModel);
        rentalTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(rentalTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Rental Records")
        );

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        );
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Button actions
        searchButton.addActionListener(
                e -> loadRentalHistory(searchField.getText().trim())
        );

        searchField.addActionListener(
                e -> loadRentalHistory(searchField.getText().trim())
        );

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadRentalHistory("");
        });
    }

    private void loadRentalHistory(String search) {
        tableModel.setRowCount(0);

        String sql =
                "SELECT r.rental_id, c.name AS customer_name, " +
                "v.vehicle_number, r.rental_date, " +
                "r.expected_return_date, r.actual_return_date, " +
                "r.rental_amount, r.fine, r.total_amount, r.status " +
                "FROM rentals r " +
                "JOIN customers c ON r.customer_id = c.customer_id " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id ";

        if (!search.isEmpty()) {
            sql += "WHERE c.name LIKE ? OR v.vehicle_number LIKE ? ";
        }

        sql += "ORDER BY r.rental_id DESC";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (!search.isEmpty()) {
                String value = "%" + search + "%";
                statement.setString(1, value);
                statement.setString(2, value);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tableModel.addRow(new Object[]{
                        result.getInt("rental_id"),
                        result.getString("customer_name"),
                        result.getString("vehicle_number"),
                        result.getDate("rental_date"),
                        result.getDate("expected_return_date"),
                        result.getDate("actual_return_date"),
                        result.getDouble("rental_amount"),
                        result.getDouble("fine"),
                        result.getDouble("total_amount"),
                        result.getString("status")
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load rental history:\n" + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RentalHistory().setVisible(true);
        });
    }
}
