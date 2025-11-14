package com.expensetracker.dao;

import com.expensetracker.model.Transaction;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionDAO — MySQL-friendly
 * Assumes table 'transactions' has columns:
 *   id INT AUTO_INCREMENT PRIMARY KEY,
 *   user_id INT,
 *   amount DOUBLE,
 *   category VARCHAR(...),
 *   type VARCHAR(...),
 *   date VARCHAR(...) or DATE stored as 'YYYY-MM-DD',
 *   description TEXT,
 *   FOREIGN KEY (user_id) REFERENCES users(id)
 */
public class TransactionDAO {

    public TransactionDAO() {
        // Create table if not exists (safe no-op if already created)
        String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                     "id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "user_id INT NOT NULL, " +
                     "amount DOUBLE NOT NULL, " +
                     "category VARCHAR(200), " +
                     "type VARCHAR(20) NOT NULL, " +
                     "date VARCHAR(20), " +       // storing date as string 'YYYY-MM-DD'
                     "description TEXT, " +
                     "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                     ")";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Insert a transaction (returns true if inserted)
    public boolean addTransaction(Transaction t) {
        String sql = "INSERT INTO transactions (user_id, amount, category, type, date, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, t.getUserId());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getCategory());
            ps.setString(4, t.getType());
            ps.setString(5, t.getDate());         // expects 'YYYY-MM-DD' or any string date
            ps.setString(6, t.getDescription());

            int rows = ps.executeUpdate();
            if (rows == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Fetch all transactions for a user (ordered newest first)
    public List<Transaction> getTransactionsByUser(int userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT id, user_id, amount, category, type, date, description FROM transactions WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = new Transaction(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getDouble("amount"),
                            rs.getString("category"),
                            rs.getString("type"),
                            rs.getString("description"),
                            rs.getString("date")
                    );
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Sum of amounts by type (EXPENSE or INCOME)
    public double getTotalByType(int userId, String type) {
        String sql = "SELECT SUM(amount) AS total FROM transactions WHERE user_id = ? AND type = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Category totals for pie chart — returns List<Object[]> where each row: [String category, Double total]
    public List<Object[]> getCategoryTotals(int userId) {
        List<Object[]> results = new ArrayList<>();
        String sql = "SELECT category, SUM(amount) AS total FROM transactions WHERE user_id = ? AND type = 'EXPENSE' GROUP BY category";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Object[]{rs.getString("category"), rs.getDouble("total")});
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // Monthly totals for bar chart — returns List<Object[]> rows: [String month, Double total], month 'YYYY-MM'
    public List<Object[]> getMonthlyTotals(int userId) {
        List<Object[]> results = new ArrayList<>();
        // use SUBSTRING to extract YYYY-MM from a 'YYYY-MM-DD' string date; adjust if date is stored differently
        String sql = "SELECT SUBSTRING(date,1,7) AS month, SUM(amount) AS total FROM transactions WHERE user_id = ? AND type = 'EXPENSE' GROUP BY month ORDER BY month";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Object[]{rs.getString("month"), rs.getDouble("total")});
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}


