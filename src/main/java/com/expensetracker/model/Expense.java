package com.expensetracker.model;

import java.sql.Timestamp;

public class Expense {
    private int id;
    private double amount;
    private String category;
    private String description;
    private Timestamp date;

    public Expense() {}

    public Expense(double amount, String category, String description) {
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public Expense(int id, double amount, String category, String description, Timestamp date) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.sql.Timestamp getDate() { return date; }
    public void setDate(java.sql.Timestamp date) { this.date = date; }
}

