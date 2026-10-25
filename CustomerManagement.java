
package com.mycompany.vehiclerentalsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CustomerManagement extends JFrame {

    private JTextField nameField, phoneField, emailField;
    private JTextField addressField, licenseField, searchField;
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private int selectedCustomerId = -1;

    public CustomerManagement() {
        setTitle("Customer Management");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        createInterface();
        loadCustomers();
    }

    private void createInterface() {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("CUSTOMER MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        // Customer input fields
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Customer Details"));

        nameField = new JTextField();
        phoneField = new JTextField();
        emailField = new JTextField();
        addressField = new JTextField();
        licenseField = new JTextField();

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Phone:"));
        inputPanel.add(phoneField);
        inputPanel.add(new JLabel("Email:"));
        inputPanel.add(emailField);
        inputPanel.add(new JLabel("Address:"));
        inputPanel.add(addressField);
        inputPanel.add(new JLabel("License Number:"));
        inputPanel.add(licenseField);

        // Buttons
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
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
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Customer"));
        searchPanel.add(new JLabel("Name / Phone / License:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel();
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        // Customer table
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Name", "Phone", "Email",
                    "Address", "License Number"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(tableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Customer List"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // Button actions
        addButton.addActionListener(e -> addCustomer());
        updateButton.addActionListener(e -> updateCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());
        clearButton.addActionListener(e -> clearFields());
        searchButton.addActionListener(e -> searchCustomers());

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadCustomers();
        });

        // Select a customer from the table
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = customerTable.getSelectedRow();

                if (row >= 0) {
                    selectedCustomerId = Integer.parseInt(
                            tableModel.getValueAt(row, 0).toString());

                    nameField.setText(tableModel.getValueAt(row, 1).toString());
                    phoneField.setText(tableModel.getValueAt(row, 2).toString());
                    emailField.setText(tableModel.getValueAt(row, 3).toString());
                    addressField.setText(tableModel.getValueAt(row, 4).toString());
                    licenseField.setText(tableModel.getValueAt(row, 5).toString());
                }
            }
        });
    }

    // Add a customer
    private void addCustomer() {
        if (!validateFields()) {
            return;
        }

        String sql = "INSERT INTO customers "
                + "(name, phone, email, address, license_number) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setCustomerValues(ps, false);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Customer added successfully!");
            clearFields();
            loadCustomers();

        } catch (SQLIntegrityConstraintViolationException e) {
            showError("License number already exists!");
        } catch (SQLException e) {
            showError("Database error:\n" + e.getMessage());
        }
    }

    // Update a customer
    private void updateCustomer() {
        if (selectedCustomerId == -1) {
            showError("Please select a customer from the table first.");
            return;
        }

        if (!validateFields()) {
            return;
        }

        String sql = "UPDATE customers SET name=?, phone=?, email=?, "
                + "address=?, license_number=? WHERE customer_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setCustomerValues(ps, true);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Customer updated successfully!");
            clearFields();
            loadCustomers();

        } catch (SQLIntegrityConstraintViolationException e) {
            showError("License number already exists!");
        } catch (SQLException e) {
            showError("Database error:\n" + e.getMessage());
        }
    }

    // Set customer values in the SQL statement
    private void setCustomerValues(PreparedStatement ps, boolean updating)
            throws SQLException {

        ps.setString(1, nameField.getText().trim());
        ps.setString(2, phoneField.getText().trim());
        ps.setString(3, emailField.getText().trim());
        ps.setString(4, addressField.getText().trim());
        ps.setString(5, licenseField.getText().trim());

        if (updating) {
            ps.setInt(6, selectedCustomerId);
        }
    }

    // Delete a customer
    private void deleteCustomer() {
        if (selectedCustomerId == -1) {
            showError("Please select a customer from the table first.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this customer?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM customers WHERE customer_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, selectedCustomerId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Customer deleted successfully!");
            clearFields();
            loadCustomers();

        } catch (SQLException e) {
            showError("Cannot delete this customer.\n"
                    + "The customer may already have rental records.\n\n"
                    + e.getMessage());
        }
    }

    // Load all customers
    private void loadCustomers() {
        String sql = "SELECT customer_id, name, phone, email, address, "
                + "license_number FROM customers ORDER BY customer_id";

        tableModel.setRowCount(0);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                addTableRow(rs);
            }

        } catch (SQLException e) {
            showError("Could not load customers:\n" + e.getMessage());
        }
    }

    // Search customers by name, phone or license number
    private void searchCustomers() {
        String search = searchField.getText().trim();

        if (search.isEmpty()) {
            loadCustomers();
            return;
        }

        String sql = "SELECT customer_id, name, phone, email, address, "
                + "license_number FROM customers "
                + "WHERE name LIKE ? OR phone LIKE ? OR license_number LIKE ? "
                + "ORDER BY customer_id";

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

    // Add a database record to the table
    private void addTableRow(ResultSet rs) throws SQLException {
        tableModel.addRow(new Object[]{
            rs.getInt("customer_id"),
            rs.getString("name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("address"),
            rs.getString("license_number")
        });
    }

    // Validate required fields
    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty()
                || phoneField.getText().trim().isEmpty()
                || addressField.getText().trim().isEmpty()
                || licenseField.getText().trim().isEmpty()) {

            showError("Please fill in all required fields.");
            return false;
        }

        return true;
    }

    // Clear the fields
    private void clearFields() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        licenseField.setText("");

        selectedCustomerId = -1;
        customerTable.clearSelection();
    }

    // Display an error message
    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CustomerManagement window = new CustomerManagement();
            window.setVisible(true);
        });
    }
}
