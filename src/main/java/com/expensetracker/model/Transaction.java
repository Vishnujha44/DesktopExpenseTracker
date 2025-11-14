package com.expensetracker.model;

/**
 * Transaction model used by the DAO and UI.
 * Fields:
 *   id, userId, amount, category, type (EXPENSE/INCOME), description, date (String "YYYY-MM-DD")
 *
 * Constructors and getters provided to match DAO usage.
 */
public class Transaction {
    private int id;
    private int userId;
    private double amount;
    private String category;
    private String type;        // "EXPENSE" or "INCOME"
    private String description; // previously 'note'
    private String date;        // format 'YYYY-MM-DD' (string for DB compatibility)

    public Transaction() {}

    // Constructor used when creating new transaction (no id yet)
    public Transaction(int userId, double amount, String category, String type, String description, String date) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.description = description;
        this.date = date;
    }

    // Full constructor (from DB)
    public Transaction(int id, int userId, double amount, String category, String type, String description, String date) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.description = description;
        this.date = date;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // DAO expects getDescription()
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // DAO expects getDate()
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
