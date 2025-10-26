package com.expensetracker.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DBConnection {
    private static Connection conn;

    // Update these credentials for your MySQL server
    private static final String URL = "jdbc:mysql://localhost:3306/expense_tracker";
    private static final String USER = "root";
    private static final String PASS = "12345678";

    public static Connection getConnection() {
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(URL, USER, PASS);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        return conn;
    }
}

