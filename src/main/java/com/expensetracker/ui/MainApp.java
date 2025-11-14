package com.expensetracker.ui;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.dao.BudgetDAO;
import com.expensetracker.export.CSVExporter;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.User;
import com.expensetracker.model.BudgetGoal;
import com.expensetracker.util.SecurityUtil;
import com.expensetracker.util.BackupUtil;
import com.expensetracker.ui.ChartPanel;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class MainApp extends JFrame {
    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO txDAO = new TransactionDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();
    private User loggedIn;

    private JTable table;
    private DefaultTableModel model;
    private JLabel lblBalance, lblExpense, lblIncome, lblBudgetWarning;
    private ChartPanel chartPanel;

    public MainApp() {
        setTitle("Desktop Expense Tracker - JavArena ");
        setSize(1100, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initLogin();
    }

    private void initLogin() {
        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register");

        p.add(new JLabel("Username:"));
        p.add(user);
        p.add(new JLabel("Password:"));
        p.add(pass);
        p.add(loginBtn);
        p.add(regBtn);

        setContentPane(p);

        loginBtn.addActionListener(e -> {
            String u = user.getText().trim();
            String pw = new String(pass.getPassword());
            User found = userDAO.findByUsername(u);
            if (found != null && found.getPasswordHash().equals(SecurityUtil.sha256(pw))) {
                loggedIn = found;
                initMainUI();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        });

        regBtn.addActionListener(e -> {
            String u = user.getText().trim();
            String pw = new String(pass.getPassword());
            if (u.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username & password");
                return;
            }
            User exists = userDAO.findByUsername(u);
            if (exists != null) {
                JOptionPane.showMessageDialog(this, "Username taken");
                return;
            }
            User newUser = new User(u, SecurityUtil.sha256(pw));
            if (userDAO.addUser(newUser)) JOptionPane.showMessageDialog(this, "Registered. You can login now.");
            else JOptionPane.showMessageDialog(this, "Registration failed.");
        });
    }

    private void initMainUI() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.DARK_GRAY);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        stats.setOpaque(false);

        lblBalance = new JLabel("Balance: 0.00");
        lblExpense = new JLabel("Total Expense: 0.00");
        lblIncome = new JLabel("Total Income: 0.00");
        lblBudgetWarning = new JLabel("");
        lblBudgetWarning.setForeground(Color.ORANGE);

        lblBalance.setForeground(Color.WHITE);
        lblExpense.setForeground(Color.WHITE);
        lblIncome.setForeground(Color.WHITE);

        stats.add(lblBalance);
        stats.add(lblIncome);
        stats.add(lblExpense);
        stats.add(lblBudgetWarning);

        top.add(stats, BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Txn");
        JButton exportBtn = new JButton("Export CSV");
        JButton chartsBtn = new JButton("View Charts");
        JButton exportChartBtn = new JButton("Export Chart PNG");
        JButton backupBtn = new JButton("Backup DB");
        JButton budgetBtn = new JButton("Set Budget Goal");

        JPanel actions = new JPanel();
        actions.add(addBtn);
        actions.add(exportBtn);
        actions.add(chartsBtn);
        actions.add(exportChartBtn);
        actions.add(budgetBtn);
        actions.add(backupBtn);
        actions.setOpaque(false);

        top.add(actions, BorderLayout.EAST);

        // Note header uses Description (matching new model)
        model = new DefaultTableModel(new Object[]{"ID", "Type", "Amount", "Category", "Description", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);

        chartPanel = new ChartPanel();
        chartPanel.setPreferredSize(new Dimension(450, 400));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp, chartPanel);
        split.setDividerLocation(620);

        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);
        revalidate();
        repaint();

        refreshData();

        addBtn.addActionListener(e -> showAddDialog());
        exportBtn.addActionListener(e -> exportCSV());
        chartsBtn.addActionListener(e -> chartPanel.repaint());
        exportChartBtn.addActionListener(e -> exportChartPNG());
        budgetBtn.addActionListener(e -> showBudgetDialog());
        backupBtn.addActionListener(e -> backupDatabase());
    }

    private void refreshData() {
        model.setRowCount(0);

        // NOTE: using DAO method getTransactionsByUser(...) which returns List<Transaction>
        List<Transaction> list = txDAO.getTransactionsByUser(loggedIn.getId());
        double totalExp = 0, totalInc = 0;
        for (Transaction t : list) {
            model.addRow(new Object[]{
                    t.getId(),
                    t.getType(),
                    t.getAmount(),
                    t.getCategory(),
                    t.getDescription(),   // new field name
                    t.getDate()           // new field name (string)
            });
            if ("EXPENSE".equals(t.getType())) totalExp += t.getAmount();
            else totalInc += t.getAmount();
        }

        lblExpense.setText(String.format("Total Expense: %.2f", totalExp));
        lblIncome.setText(String.format("Total Income: %.2f", totalInc));
        lblBalance.setText(String.format("Balance: %.2f", totalInc - totalExp));

        // ChartPanel should be compatible with your DAO (it should use txDAO.getCategoryTotals/getMonthlyTotals)
        chartPanel.setUserId(loggedIn.getId());
        chartPanel.setDAO(txDAO);
        chartPanel.repaint();

        checkBudgets();
    }

    private void checkBudgets() {
        try {
            // BudgetDAO now returns List<BudgetGoal>
            List<BudgetGoal> goals = budgetDAO.getGoalsForUser(loggedIn.getId());
            // get category totals as List<Object[]>
            List<Object[]> catTotals = txDAO.getCategoryTotals(loggedIn.getId());

            for (BudgetGoal g : goals) {
                String category = g.getCategory();
                double limit = g.getMonthlyLimit();

                // find total for this category in catTotals
                double total = 0.0;
                for (Object[] row : catTotals) {
                    String cat = row[0] == null ? "Uncategorized" : row[0].toString();
                    double v = row[1] == null ? 0.0 : Double.parseDouble(row[1].toString());
                    if (cat.equals(category)) {
                        total = v;
                        break;
                    }
                }

                if (total > limit) {
                    lblBudgetWarning.setText(String.format("Overspending: %s (%.2f/%.2f)", category, total, limit));
                    return;
                } else {
                    lblBudgetWarning.setText("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddDialog() {
        JTextField amountF = new JTextField();
        JTextField categoryF = new JTextField();
        JTextField descF = new JTextField();
        String[] types = {"EXPENSE", "INCOME"};
        JComboBox<String> typeBox = new JComboBox<>(types);
        Object[] fields = {"Type:", typeBox, "Amount:", amountF, "Category:", categoryF, "Description:", descF};
        int ok = JOptionPane.showConfirmDialog(this, fields, "Add Transaction", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                double amt = Double.parseDouble(amountF.getText().trim());
                String cat = categoryF.getText().trim();
                String desc = descF.getText().trim();
                String type = (String) typeBox.getSelectedItem();

                // Use LocalDate.now().toString() for 'date' field (YYYY-MM-DD), matches DAO expectations
                Transaction t = new Transaction(loggedIn.getId(), amt, cat, type, desc, LocalDate.now().toString());
                if (txDAO.addTransaction(t)) refreshData();
                else JOptionPane.showMessageDialog(this, "Failed to add transaction");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount");
            }
        }
    }

    private void exportCSV() {
        // use DAO method name getTransactionsByUser
        List<Transaction> list = txDAO.getTransactionsByUser(loggedIn.getId());
        String path = JOptionPane.showInputDialog(this, "Enter path to save CSV:", "transactions.csv");
        if (path == null || path.trim().isEmpty()) return;
        if (CSVExporter.exportTransactions(path, list)) JOptionPane.showMessageDialog(this, "Exported to " + path);
        else JOptionPane.showMessageDialog(this, "Export failed");
    }

    private void exportChartPNG() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("charts.png"));
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (chartPanel.exportAsImage(f)) JOptionPane.showMessageDialog(this, "Chart exported to " + f.getAbsolutePath());
            else JOptionPane.showMessageDialog(this, "Chart export failed");
        }
    }

    private void showBudgetDialog() {
        JTextField categoryF = new JTextField();
        JTextField limitF = new JTextField();
        Object[] fields = {"Category:", categoryF, "Monthly Limit:", limitF};
        int ok = JOptionPane.showConfirmDialog(this, fields, "Set Budget Goal", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                String cat = categoryF.getText().trim();
                double lim = Double.parseDouble(limitF.getText().trim());
                BudgetGoal g = new BudgetGoal(loggedIn.getId(), cat, lim);
                if (budgetDAO.addOrUpdateGoal(g)) {
                    JOptionPane.showMessageDialog(this, "Budget saved");
                    refreshData();
                } else JOptionPane.showMessageDialog(this, "Failed to save goal");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid limit");
            }
        }
    }

    private void backupDatabase() {
        // If you are using SQLite, keep same path; if MySQL, backup operation will be different.
        String dbPath = "expense_tracker_final.db";
        String dest = JOptionPane.showInputDialog(this, "Enter backup path:", "backup.db");
        if (dest == null || dest.trim().isEmpty()) return;
        if (BackupUtil.backupDatabase(dbPath, dest)) JOptionPane.showMessageDialog(this, "Backup saved to " + dest);
        else JOptionPane.showMessageDialog(this, "Backup failed");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainApp app = new MainApp();
            app.setVisible(true);
        });
    }
}

