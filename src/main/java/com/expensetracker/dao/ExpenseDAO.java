package com.expensetracker.dao;

import com.expensetracker.model.Expense;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    private final Connection conn;

    public ExpenseDAO() {
        this.conn = DBConnection.getConnection();
    }

    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses ORDER BY date DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Expense(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getTimestamp("date")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addExpense(Expense e) throws SQLException {
        String sql = "INSERT INTO expenses(amount, category, description) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, e.getAmount());
            ps.setString(2, e.getCategory());
            ps.setString(3, e.getDescription());
            ps.executeUpdate();
        }
    }

    public void updateExpense(Expense e) throws SQLException {
        String sql = "UPDATE expenses SET amount=?, category=?, description=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, e.getAmount());
            ps.setString(2, e.getCategory());
            ps.setString(3, e.getDescription());
            ps.setInt(4, e.getId());
            ps.executeUpdate();
        }
    }

    public void deleteExpense(int id) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Expense> filter(String category, String fromDate, String toDate) {
        List<Expense> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM expenses WHERE 1=1");
        if (category != null && !category.isEmpty()) sb.append(" AND category = ?");
        if (fromDate != null && !fromDate.isEmpty()) sb.append(" AND date >= ?");
        if (toDate != null && !toDate.isEmpty()) sb.append(" AND date <= ?");
        sb.append(" ORDER BY date DESC");
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int idx = 1;
            if (category != null && !category.isEmpty()) ps.setString(idx++, category);
            if (fromDate != null && !fromDate.isEmpty()) ps.setString(idx++, fromDate);
            if (toDate != null && !toDate.isEmpty()) ps.setString(idx++, toDate + " 23:59:59");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Expense(
                            rs.getInt("id"),
                            rs.getDouble("amount"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getTimestamp("date")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Methods for aggregated data used by charts
    public ResultSet getCategoryTotals() throws SQLException {
        String sql = "SELECT category, SUM(amount) AS total FROM expenses GROUP BY category";
        Statement st = conn.createStatement();
        return st.executeQuery(sql); // caller must close ResultSet/Statement if needed
    }

    public ResultSet getMonthlyTotals() throws SQLException {
        String sql = "SELECT DATE_FORMAT(date, '%Y-%m') AS month, SUM(amount) AS total FROM expenses GROUP BY month ORDER BY month";
        Statement st = conn.createStatement();
        return st.executeQuery(sql);
    }
}
