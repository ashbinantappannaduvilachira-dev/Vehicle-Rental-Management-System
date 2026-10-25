
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReturnVehicle extends JFrame {

    private JTable rentalTable;
    private DefaultTableModel tableModel;

    private int selectedRentalId = -1;
    private double selectedRentalAmount;
    private String selectedExpectedReturnDate;

    public ReturnVehicle() {
        setTitle("Return Vehicle");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadRentedVehicles();
    }

    private void createInterface() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("RETURN VEHICLE", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{
                    "Rental ID", "Customer", "Vehicle",
                    "Rental Date", "Expected Return",
                    "Rental Amount", "Status"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        rentalTable = new JTable(tableModel);
        rentalTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        JScrollPane scrollPane = new JScrollPane(rentalTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        "Currently Rented Vehicles"
                )
        );

        add(scrollPane, BorderLayout.CENTER);

        JButton returnButton = new JButton("Return Selected Vehicle");
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.add(returnButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        rentalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = rentalTable.getSelectedRow();

                if (row >= 0) {
                    selectedRentalId =
                            Integer.parseInt(tableModel.getValueAt(row, 0).toString());

                    selectedExpectedReturnDate =
                            tableModel.getValueAt(row, 4).toString();

                    selectedRentalAmount =
                            Double.parseDouble(tableModel.getValueAt(row, 5).toString());
                } else {
                    selectedRentalId = -1;
                }
            }
        });

        returnButton.addActionListener(e -> returnVehicle());
        refreshButton.addActionListener(e -> loadRentedVehicles());
        closeButton.addActionListener(e -> dispose());
    }

    private void loadRentedVehicles() {
        tableModel.setRowCount(0);
        selectedRentalId = -1;

        String sql =
                "SELECT r.rental_id, c.name AS customer_name, " +
                "v.vehicle_number, r.rental_date, " +
                "r.expected_return_date, r.rental_amount, r.status " +
                "FROM rentals r " +
                "JOIN customers c ON r.customer_id = c.customer_id " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                "WHERE r.status = 'Rented' " +
                "ORDER BY r.rental_id";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                tableModel.addRow(new Object[]{
                    result.getInt("rental_id"),
                    result.getString("customer_name"),
                    result.getString("vehicle_number"),
                    result.getDate("rental_date"),
                    result.getDate("expected_return_date"),
                    result.getDouble("rental_amount"),
                    result.getString("status")
                });
            }
        } catch (SQLException e) {
            showError("Could not load rented vehicles:\n" + e.getMessage());
        }
    }

    private void returnVehicle() {
        if (selectedRentalId == -1) {
            JOptionPane.showMessageDialog(
                    this, "Please select a rented vehicle first."
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to return this vehicle?",
                "Confirm Return",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        LocalDate actualReturnDate = LocalDate.now();
        LocalDate expectedReturnDate;

        try {
            expectedReturnDate = LocalDate.parse(
                    selectedExpectedReturnDate
            );
        } catch (Exception e) {
            showError("Invalid expected return date.");
            return;
        }

        long overdueDays = ChronoUnit.DAYS.between(
                expectedReturnDate, actualReturnDate
        );

        overdueDays = Math.max(overdueDays, 0);

        String rentalInfoSql =
                "SELECT r.vehicle_id, r.rental_amount, v.daily_rate " +
                "FROM rentals r " +
                "JOIN vehicles v ON r.vehicle_id = v.vehicle_id " +
                "WHERE r.rental_id = ? AND r.status = 'Rented'";

        String updateRentalSql =
                "UPDATE rentals SET actual_return_date = ?, fine = ?, " +
                "total_amount = ?, status = 'Returned' " +
                "WHERE rental_id = ? AND status = 'Rented'";

        String updateVehicleSql =
                "UPDATE vehicles SET status = 'Available' " +
                "WHERE vehicle_id = ?";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int vehicleId;
                double rentalAmount;
                double dailyRate;

                // Get rental and vehicle information.
                try (PreparedStatement statement =
                             connection.prepareStatement(rentalInfoSql)) {

                    statement.setInt(1, selectedRentalId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException(
                                    "Rental record not found or already returned."
                            );
                        }

                        vehicleId = result.getInt("vehicle_id");
                        rentalAmount = result.getDouble("rental_amount");
                        dailyRate = result.getDouble("daily_rate");
                    }
                }

                // Calculate fine and total amount.
                double fine = overdueDays * dailyRate;
                double totalAmount = rentalAmount + fine;

                // Update rental details.
                try (PreparedStatement statement =
                             connection.prepareStatement(updateRentalSql)) {

                    statement.setDate(
                            1, Date.valueOf(actualReturnDate)
                    );
                    statement.setDouble(2, fine);
                    statement.setDouble(3, totalAmount);
                    statement.setInt(4, selectedRentalId);

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException(
                                "The rental could not be updated."
                        );
                    }
                }

                // Make the vehicle available again.
                try (PreparedStatement statement =
                             connection.prepareStatement(updateVehicleSql)) {

                    statement.setInt(1, vehicleId);

                    if (statement.executeUpdate() == 0) {
                        throw new SQLException(
                                "The vehicle status could not be updated."
                        );
                    }
                }

                connection.commit();

                String message =
                        "Vehicle returned successfully!\n\n" +
                        "Overdue Days: " + overdueDays +
                        "\nFine: ₹" + String.format("%.2f", fine) +
                        "\nTotal Amount: ₹" +
                        String.format("%.2f", totalAmount);

                JOptionPane.showMessageDialog(
                        this,
                        message,
                        "Return Successful",
                        JOptionPane.INFORMATION_MESSAGE
                );

                loadRentedVehicles();

            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;

            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    // The connection will close automatically.
                }
            }

        } catch (SQLException e) {
            showError("Return failed:\n" + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ReturnVehicle().setVisible(true);
        });
    }
}
