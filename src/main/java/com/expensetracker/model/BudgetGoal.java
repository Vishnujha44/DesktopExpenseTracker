package com.expensetracker.model;
public class BudgetGoal {
    private int id; private int userId; private String category; private double monthlyLimit;
    public BudgetGoal() {}
    public BudgetGoal(int userId,String category,double monthlyLimit){this.userId=userId;this.category=category;this.monthlyLimit=monthlyLimit;}
    public BudgetGoal(int id,int userId,String category,double monthlyLimit){this.id=id;this.userId=userId;this.category=category;this.monthlyLimit=monthlyLimit;}
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getUserId(){return userId;} public void setUserId(int uid){this.userId=uid;}
    public String getCategory(){return category;} public void setCategory(String c){this.category=c;}
    public double getMonthlyLimit(){return monthlyLimit;} public void setMonthlyLimit(double m){this.monthlyLimit=m;}
}