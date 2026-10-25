
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class VehicleManagement extends JFrame {

    private JTextField vehicleNumberField, brandField, modelField;
    private JTextField typeField, rateField, searchField;
    private JTable vehicleTable;
    private DefaultTableModel tableModel;
    private int selectedVehicleId = -1;

    public VehicleManagement() {
        setTitle("Vehicle Management");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadVehicles();
    }

    private void createInterface() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("VEHICLE MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        // Vehicle input fields
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Details"));

        vehicleNumberField = new JTextField();
        brandField = new JTextField();
        modelField = new JTextField();
        typeField = new JTextField();
        rateField = new JTextField();

        inputPanel.add(new JLabel("Vehicle Number:"));
        inputPanel.add(vehicleNumberField);
        inputPanel.add(new JLabel("Brand:"));
        inputPanel.add(brandField);
        inputPanel.add(new JLabel("Model:"));
        inputPanel.add(modelField);
        inputPanel.add(new JLabel("Vehicle Type:"));
        inputPanel.add(typeField);
        inputPanel.add(new JLabel("Daily Rate:"));
        inputPanel.add(rateField);

        // Buttons
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Search panel
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Vehicle"));
        searchPanel.add(new JLabel("Number / Brand / Model:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel();
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        // Vehicle table
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Vehicle Number", "Brand", "Model",
                    "Type", "Daily Rate", "Status"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        vehicleTable = new JTable(tableModel);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(vehicleTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Vehicle List"));

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(searchPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        // Put the table and search panel together
        JPanel tablePanel = new JPanel(new BorderLayout(10, 10));
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.remove(searchPanel);
        mainPanel.remove(scrollPane);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Button actions
        addButton.addActionListener(e -> addVehicle());
        updateButton.addActionListener(e -> updateVehicle());
        deleteButton.addActionListener(e -> deleteVehicle());
        clearButton.addActionListener(e -> clearFields());
        searchButton.addActionListener(e -> searchVehicles());

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadVehicles();
        });

        // Select a vehicle from the table
        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = vehicleTable.getSelectedRow();

                if (row >= 0) {
                    selectedVehicleId = Integer.parseInt(
                            tableModel.getValueAt(row, 0).toString());

                    vehicleNumberField.setText(
                            tableModel.getValueAt(row, 1).toString());
                    brandField.setText(
                            tableModel.getValueAt(row, 2).toString());
                    modelField.setText(
                            tableModel.getValueAt(row, 3).toString());
                    typeField.setText(
                            tableModel.getValueAt(row, 4).toString());
                    rateField.setText(
                            tableModel.getValueAt(row, 5).toString());
                }
            }
        });
    }

    // Add a vehicle
    private void addVehicle() {
        if (!validateFields()) {
            return;
        }

        String sql = "INSERT INTO vehicles "
                + "(vehicle_number, brand, model, vehicle_type, daily_rate) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setVehicleValues(ps, false);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Vehicle added successfully!");
            clearFields();
            loadVehicles();

        } catch (SQLIntegrityConstraintViolationException e) {
            showError("Vehicle number already exists!");
        } catch (SQLException e) {
            showError("Database error:\n" + e.getMessage());
        }
    }

    // Update a vehicle
    private void updateVehicle() {
        if (selectedVehicleId == -1) {
            showError("Please select a vehicle from the table first.");
            return;
        }

        if (!validateFields()) {
            return;
        }

        String sql = "UPDATE vehicles SET vehicle_number=?, brand=?, "
                + "model=?, vehicle_type=?, daily_rate=? WHERE vehicle_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setVehicleValues(ps, true);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Vehicle updated successfully!");
            clearFields();
            loadVehicles();

        } catch (SQLIntegrityConstraintViolationException e) {
            showError("Vehicle number already exists!");
        } catch (SQLException e) {
            showError("Database error:\n" + e.getMessage());
        }
    }

    // Set the vehicle values in the SQL statement
    private void setVehicleValues(PreparedStatement ps, boolean updating)
            throws SQLException {

        ps.setString(1, vehicleNumberField.getText().trim());
        ps.setString(2, brandField.getText().trim());
        ps.setString(3, modelField.getText().trim());
        ps.setString(4, typeField.getText().trim());
        ps.setDouble(5, Double.parseDouble(rateField.getText().trim()));

        if (updating) {
            ps.setInt(6, selectedVehicleId);
        }
    }

    // Delete a vehicle
    private void deleteVehicle() {
        if (selectedVehicleId == -1) {
            showError("Please select a vehicle from the table first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this vehicle?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM vehicles WHERE vehicle_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, selectedVehicleId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Vehicle deleted successfully!");
            clearFields();
            loadVehicles();

        } catch (SQLException e) {
            showError("Cannot delete this vehicle.\n"
                    + "It may already have rental records.\n\n"
                    + e.getMessage());
        }
    }

    // Load all vehicles
    private void loadVehicles() {
        String sql = "SELECT vehicle_id, vehicle_number, brand, model, "
                + "vehicle_type, daily_rate, status FROM vehicles "
                + "ORDER BY vehicle_id";

        tableModel.setRowCount(0);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                addTableRow(rs);
            }

        } catch (SQLException e) {
            showError("Could not load vehicles:\n" + e.getMessage());
        }
    }

    // Search vehicles by number, brand or model
    private void searchVehicles() {
        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            loadVehicles();
            return;
        }

        String sql = "SELECT vehicle_id, vehicle_number, brand, model, "
                + "vehicle_type, daily_rate, status FROM vehicles "
                + "WHERE vehicle_number LIKE ? OR brand LIKE ? OR model LIKE ? "
                + "ORDER BY vehicle_id";

        tableModel.setRowCount(0);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String value = "%" + search + "%";

            ps.setString(1, value);
            ps.setString(2, value);
            ps.setString(3, value);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addTableRow(rs);
                }
            }

        } catch (SQLException e) {
            showError("Search failed:\n" + e.getMessage());
        }
    }

    // Add one database record to the table
    private void addTableRow(ResultSet rs) throws SQLException {
        tableModel.addRow(new Object[]{
            rs.getInt("vehicle_id"),
            rs.getString("vehicle_number"),
            rs.getString("brand"),
            rs.getString("model"),
            rs.getString("vehicle_type"),
            rs.getDouble("daily_rate"),
            rs.getString("status")
        });
    }

    // Validate the input fields
    private boolean validateFields() {
        if (vehicleNumberField.getText().trim().isEmpty()
                || brandField.getText().trim().isEmpty()
                || modelField.getText().trim().isEmpty()
                || typeField.getText().trim().isEmpty()
                || rateField.getText().trim().isEmpty()) {

            showError("Please fill in all vehicle details.");
            return false;
        }

        try {
            double rate = Double.parseDouble(rateField.getText().trim());

            if (rate <= 0) {
                showError("Daily rate must be greater than 0.");
                return false;
            }

        } catch (NumberFormatException e) {
            showError("Daily rate must be a valid number.");
            return false;
        }

        return true;
    }

    // Clear the fields
    private void clearFields() {
        vehicleNumberField.setText("");
        brandField.setText("");
        modelField.setText("");
        typeField.setText("");
        rateField.setText("");

        selectedVehicleId = -1;
        vehicleTable.clearSelection();
    }

    // Display an error message
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VehicleManagement window = new VehicleManagement();
            window.setVisible(true);
        });
    }
}
