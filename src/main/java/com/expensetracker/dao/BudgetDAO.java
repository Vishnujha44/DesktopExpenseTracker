package com.expensetracker.dao;

import com.expensetracker.model.BudgetGoal;
import com.expensetracker.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BudgetDAO handles storing and retrieving per-user monthly budget goals.
 * Table used: budget_goals (id, user_id, category, monthly_limit)
 */
public class BudgetDAO {

    public BudgetDAO() {
        // create table if it doesn't exist (works for both SQLite/MySQL with this SQL)
        String sql = """
                CREATE TABLE IF NOT EXISTS budget_goals (
                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                    user_id INTEGER NOT NULL,
                    category VARCHAR(255),
                    monthly_limit DOUBLE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """;

        // Note: If using SQLite, the AUTO_INCREMENT token will need to be changed to AUTOINCREMENT.
        // If you run into DB engine issues, replace line above with engine-appropriate create-table SQL.
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException ex) {
            // print to console for debugging — replace with a logger if you prefer
            ex.printStackTrace();
        }
    }

    /**
     * Add or update (upsert) a budget goal for a user+category.
     * Simpler approach: delete existing then insert new.
     */
    public boolean addOrUpdateGoal(BudgetGoal g) {
        String deleteSql = "DELETE FROM budget_goals WHERE user_id = ? AND category = ?";
        String insertSql = "INSERT INTO budget_goals(user_id, category, monthly_limit) VALUES(?,?,?)";

        try (Connection conn = DBConnection.getConnection()) {
            // delete existing (if any)
            try (PreparedStatement pd = conn.prepareStatement(deleteSql)) {
                pd.setInt(1, g.getUserId());
                pd.setString(2, g.getCategory());
                pd.executeUpdate();
            }

            // insert new
            try (PreparedStatement pi = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pi.setInt(1, g.getUserId());
                pi.setString(2, g.getCategory());
                pi.setDouble(3, g.getMonthlyLimit());
                int rows = pi.executeUpdate();
                if (rows == 0) return false;
                try (ResultSet rs = pi.getGeneratedKeys()) {
                    if (rs.next()) g.setId(rs.getInt(1));
                }
                return true;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Return all budget goals for the user.
     * MainApp previously read a ResultSet — here we return a List<BudgetGoal> which is safer.
     * If you still need a ResultSet for compatibility, let me know and I will provide it instead.
     */
    public List<BudgetGoal> getGoalsForUser(int userId) {
        List<BudgetGoal> list = new ArrayList<>();
        String sql = "SELECT id, category, monthly_limit FROM budget_goals WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BudgetGoal g = new BudgetGoal(
                            rs.getInt("id"),
                            userId,
                            rs.getString("category"),
                            rs.getDouble("monthly_limit")
                    );
                    list.add(g);
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    /**
     * Fetch a single budget goal for a user+category. Returns null if none set.
     */
    public BudgetGoal getGoalForCategory(int userId, String category) {
        String sql = "SELECT id, monthly_limit FROM budget_goals WHERE user_id = ? AND category = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BudgetGoal(rs.getInt("id"), userId, category, rs.getDouble("monthly_limit"));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}


