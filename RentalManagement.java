
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalManagement extends JFrame {

    private JComboBox<CustomerItem> customerComboBox;
    private JComboBox<VehicleItem> vehicleComboBox;

    private JTextField rentalDateField;
    private JTextField returnDateField;
    private JTextField amountField;

    public RentalManagement() {
        setTitle("Vehicle Rental");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadCustomers();
        loadAvailableVehicles();

        rentalDateField.setText(LocalDate.now().toString());
    }

    // Create the GUI
    private void createInterface() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("VEHICLE RENTAL", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 15, 15));
        formPanel.setBorder(
                BorderFactory.createEmptyBorder(30, 50, 20, 50));

        customerComboBox = new JComboBox<>();
        vehicleComboBox = new JComboBox<>();
        rentalDateField = new JTextField();
        returnDateField = new JTextField();
        amountField = new JTextField();
        amountField.setEditable(false);

        formPanel.add(new JLabel("Customer:"));
        formPanel.add(customerComboBox);
        formPanel.add(new JLabel("Vehicle:"));
        formPanel.add(vehicleComboBox);
        formPanel.add(new JLabel("Rental Date (YYYY-MM-DD):"));
        formPanel.add(rentalDateField);
        formPanel.add(new JLabel("Expected Return Date (YYYY-MM-DD):"));
        formPanel.add(returnDateField);
        formPanel.add(new JLabel("Rental Amount:"));
        formPanel.add(amountField);

        add(formPanel, BorderLayout.CENTER);

        JButton calculateButton = new JButton("Calculate Amount");
        JButton rentButton = new JButton("Rent Vehicle");
        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(calculateButton);
        buttonPanel.add(rentButton);
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);

        calculateButton.addActionListener(e -> calculateRentalAmount());
        rentButton.addActionListener(e -> rentVehicle());
        clearButton.addActionListener(e -> clearFields());
    }

    // Load customers into the dropdown
    private void loadCustomers() {
        customerComboBox.removeAllItems();

        String sql = "SELECT customer_id, name, license_number "
                + "FROM customers ORDER BY name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                customerComboBox.addItem(new CustomerItem(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getString("license_number")));
            }

        } catch (SQLException e) {
            showError("Could not load customers:\n" + e.getMessage());
        }
    }

    // Load available vehicles into the dropdown
    private void loadAvailableVehicles() {
        vehicleComboBox.removeAllItems();

        String sql = "SELECT vehicle_id, vehicle_number, brand, model, daily_rate "
                + "FROM vehicles WHERE status = 'Available' "
                + "ORDER BY vehicle_number";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vehicleComboBox.addItem(new VehicleItem(
                        rs.getInt("vehicle_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getDouble("daily_rate")));
            }

        } catch (SQLException e) {
            showError("Could not load available vehicles:\n" + e.getMessage());
        }
    }

    // Calculate rental amount
    private void calculateRentalAmount() {
        if (vehicleComboBox.getSelectedItem() == null) {
            showError("Please select a vehicle.");
            return;
        }

        try {
            LocalDate rentalDate =
                    LocalDate.parse(rentalDateField.getText().trim());

            LocalDate returnDate =
                    LocalDate.parse(returnDateField.getText().trim());

            if (returnDate.isBefore(rentalDate)) {
                showError("Return date cannot be before rental date.");
                return;
            }

            long days = ChronoUnit.DAYS.between(rentalDate, returnDate);

            if (days == 0) {
                days = 1;
            }

            VehicleItem vehicle =
                    (VehicleItem) vehicleComboBox.getSelectedItem();

            double amount = days * vehicle.dailyRate;
            amountField.setText(String.format("%.2f", amount));

        } catch (Exception e) {
            showError("Please enter valid dates.\nFormat: YYYY-MM-DD");
        }
    }

    // Rent a vehicle
    private void rentVehicle() {
        if (customerComboBox.getSelectedItem() == null) {
            showError("Please select a customer.");
            return;
        }

        if (vehicleComboBox.getSelectedItem() == null) {
            showError("Please select a vehicle.");
            return;
        }

        if (amountField.getText().trim().isEmpty()) {
            calculateRentalAmount();

            if (amountField.getText().trim().isEmpty()) {
                return;
            }
        }

        try {
            LocalDate rentalDate =
                    LocalDate.parse(rentalDateField.getText().trim());

            LocalDate returnDate =
                    LocalDate.parse(returnDateField.getText().trim());

            if (returnDate.isBefore(rentalDate)) {
                showError("Return date cannot be before rental date.");
                return;
            }

            CustomerItem customer =
                    (CustomerItem) customerComboBox.getSelectedItem();

            VehicleItem vehicle =
                    (VehicleItem) vehicleComboBox.getSelectedItem();

            double amount = Double.parseDouble(amountField.getText().trim());

            // Save the rental and update vehicle status together
            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);

                try {
                    String rentalSQL = "INSERT INTO rentals "
                            + "(customer_id, vehicle_id, rental_date, "
                            + "expected_return_date, rental_amount, fine, "
                            + "total_amount, status) "
                            + "VALUES (?, ?, ?, ?, ?, 0, ?, 'Rented')";

                    try (PreparedStatement ps = con.prepareStatement(rentalSQL)) {
                        ps.setInt(1, customer.customerId);
                        ps.setInt(2, vehicle.vehicleId);
                        ps.setDate(3, java.sql.Date.valueOf(rentalDate));
                        ps.setDate(4, java.sql.Date.valueOf(returnDate));
                        ps.setDouble(5, amount);
                        ps.setDouble(6, amount);
                        ps.executeUpdate();
                    }

                    String vehicleSQL = "UPDATE vehicles SET status = 'Rented' "
                            + "WHERE vehicle_id = ? AND status = 'Available'";

                    try (PreparedStatement ps = con.prepareStatement(vehicleSQL)) {
                        ps.setInt(1, vehicle.vehicleId);

                        if (ps.executeUpdate() == 0) {
                            throw new SQLException(
                                    "Vehicle is no longer available.");
                        }
                    }

                    con.commit();

                } catch (SQLException e) {
                    con.rollback();
                    throw e;
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Vehicle rented successfully!\n\n"
                    + "Customer: " + customer.name
                    + "\nVehicle: " + vehicle.vehicleNumber
                    + "\nRental Amount: ₹" + String.format("%.2f", amount));

            clearFields();
            loadAvailableVehicles();

        } catch (SQLException e) {
            showError("Rental failed:\n" + e.getMessage());

        } catch (Exception e) {
            showError("Invalid rental information:\n" + e.getMessage());
        }
    }

    // Clear the fields
    private void clearFields() {
        rentalDateField.setText(LocalDate.now().toString());
        returnDateField.setText("");
        amountField.setText("");

        if (customerComboBox.getItemCount() > 0) {
            customerComboBox.setSelectedIndex(0);
        }
    }

    // Display an error message
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Customer displayed in the dropdown
    private static class CustomerItem {
        int customerId;
        String name;
        String licenseNumber;

        CustomerItem(int customerId, String name, String licenseNumber) {
            this.customerId = customerId;
            this.name = name;
            this.licenseNumber = licenseNumber;
        }

        @Override
        public String toString() {
            return name + " - License: " + licenseNumber;
        }
    }

    // Vehicle displayed in the dropdown
    private static class VehicleItem {
        int vehicleId;
        String vehicleNumber;
        String brand;
        String model;
        double dailyRate;

        VehicleItem(int vehicleId, String vehicleNumber,
                    String brand, String model, double dailyRate) {
            this.vehicleId = vehicleId;
            this.vehicleNumber = vehicleNumber;
            this.brand = brand;
            this.model = model;
            this.dailyRate = dailyRate;
        }

        @Override
        public String toString() {
            return vehicleNumber + " - " + brand + " " + model
                    + " (₹" + String.format("%.2f", dailyRate) + "/day)";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RentalManagement window = new RentalManagement();
            window.setVisible(true);
        });
    }
}
