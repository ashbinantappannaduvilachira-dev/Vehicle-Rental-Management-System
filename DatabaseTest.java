
package com.mycompany.vehiclerentalsystem;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTest {

    public static void main(String[] args) {

        try (Connection connection = DBConnection.getConnection()) {

            if (connection != null && !connection.isClosed()) {
                System.out.println("Database connected successfully!");
            }

        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
