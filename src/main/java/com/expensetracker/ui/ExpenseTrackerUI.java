package com.expensetracker.ui;

import com.expensetracker.dao.ExpenseDAO;
import com.expensetracker.model.Expense;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;

import com.itextpdf.layout.element.Table;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ExpenseTrackerUI extends JFrame {

    private ExpenseDAO dao;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField amountField, categoryField, descriptionField;
    private JTextField filterCategoryField, filterFromField, filterToField;
    private JLabel totalLabel;
    private JPanel chartsPanel;

    public ExpenseTrackerUI() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
        catch (Exception ignored) {}

        dao = new ExpenseDAO();
        initUI();
        loadExpenses();
        refreshCharts();
    }

    private void initUI() {
        setTitle("Expense Tracker");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Toolbar
        JToolBar toolBar = new JToolBar();
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        JButton exportExcelBtn = new JButton("Export Excel");
        JButton exportPDFBtn = new JButton("Export PDF");
        JButton pieBtn = new JButton("Pie Chart");
        JButton barBtn = new JButton("Bar Chart");

        toolBar.add(addBtn); toolBar.add(editBtn); toolBar.add(delBtn);
        toolBar.addSeparator(); toolBar.add(exportExcelBtn); toolBar.add(exportPDFBtn);
        toolBar.addSeparator(); toolBar.add(pieBtn); toolBar.add(barBtn);

        add(toolBar, BorderLayout.NORTH);

        // Table & model
        String[] cols = {"ID", "Amount (₹)", "Category", "Description", "Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Input panel
        JPanel input = new JPanel(new GridLayout(2, 6, 8, 8));
        amountField = new JTextField();
        categoryField = new JTextField();
        descriptionField = new JTextField();
        input.add(new JLabel("Amount:")); input.add(amountField);
        input.add(new JLabel("Category:")); input.add(categoryField);
        input.add(new JLabel("Description:")); input.add(descriptionField);

        // Filter fields
        filterCategoryField = new JTextField();
        filterFromField = new JTextField();
        filterToField = new JTextField();
        input.add(new JLabel("Filter Category:")); input.add(filterCategoryField);
        input.add(new JLabel("From (yyyy-mm-dd):")); input.add(filterFromField);
        input.add(new JLabel("To (yyyy-mm-dd):")); input.add(filterToField);

        JButton applyFilter = new JButton("Apply Filter");
        JButton resetFilter = new JButton("Reset Filter");
        input.add(applyFilter); input.add(resetFilter);

        add(input, BorderLayout.SOUTH);

        // Side: charts and total
        chartsPanel = new JPanel(new GridLayout(1,2,8,8));
        totalLabel = new JLabel("Total: ₹0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JPanel side = new JPanel(new BorderLayout());
        side.add(totalLabel, BorderLayout.NORTH);
        side.add(chartsPanel, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        // Action listeners
        addBtn.addActionListener(e -> addExpense());
        editBtn.addActionListener(e -> editExpense());
        delBtn.addActionListener(e -> deleteExpense());
        exportExcelBtn.addActionListener(e -> exportToExcel());
        exportPDFBtn.addActionListener(e -> exportToPDF());
        pieBtn.addActionListener(e -> refreshCharts());
        barBtn.addActionListener(e -> refreshCharts());
        applyFilter.addActionListener(e -> applyFilter());
        resetFilter.addActionListener(e -> { clearFilters(); loadExpenses(); refreshCharts(); });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int r = table.getSelectedRow();
                if (r != -1) {
                    amountField.setText(tableModel.getValueAt(r,1).toString());
                    categoryField.setText(tableModel.getValueAt(r,2).toString());
                    descriptionField.setText(tableModel.getValueAt(r,3).toString());
                }
            }
        });
    }

    private void loadExpenses() {
        tableModel.setRowCount(0);
        List<Expense> list = dao.getAllExpenses();
        double total = 0;
        for (Expense e : list) {
            tableModel.addRow(new Object[]{ e.getId(), e.getAmount(), e.getCategory(), e.getDescription(), e.getDate() });
            total += e.getAmount();
        }
        totalLabel.setText(String.format("Total: ₹%.2f", total));
    }

    private void addExpense() {
        try {
            double amt = Double.parseDouble(amountField.getText().trim());
            String cat = categoryField.getText().trim();
            String desc = descriptionField.getText().trim();
            if (cat.isEmpty() || desc.isEmpty()) { JOptionPane.showMessageDialog(this, "Fill all fields"); return; }
            dao.addExpense(new Expense(amt, cat, desc));
            clearInput();
            loadExpenses();
            refreshCharts();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void editExpense() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row"); return; }
        int id = (int) tableModel.getValueAt(r, 0);
        try {
            double amt = Double.parseDouble(amountField.getText().trim());
            String cat = categoryField.getText().trim();
            String desc = descriptionField.getText().trim();
            if (cat.isEmpty() || desc.isEmpty()) { JOptionPane.showMessageDialog(this, "Fill all fields"); return; }
            Expense e = new Expense(id, amt, cat, desc, null);
            dao.updateExpense(e);
            clearInput();
            loadExpenses();
            refreshCharts();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void deleteExpense() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Select a row"); return; }
        int id = (int) tableModel.getValueAt(r, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected expense?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { dao.deleteExpense(id); loadExpenses(); refreshCharts(); }
            catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void applyFilter() {
        String cat = filterCategoryField.getText().trim();
        String from = filterFromField.getText().trim();
        String to = filterToField.getText().trim();
        tableModel.setRowCount(0);
        List<Expense> list = dao.filter(cat, from, to);
        double total = 0;
        for (Expense e : list) {
            tableModel.addRow(new Object[]{ e.getId(), e.getAmount(), e.getCategory(), e.getDescription(), e.getDate() });
            total += e.getAmount();
        }
        totalLabel.setText(String.format("Total: ₹%.2f", total));
        refreshCharts(); // charts will reflect entire DB; you can adapt charts to filtered data if desired
    }

    private void clearInput() {
        amountField.setText("");
        categoryField.setText("");
        descriptionField.setText("");
    }

    private void clearFilters() {
        filterCategoryField.setText("");
        filterFromField.setText("");
        filterToField.setText("");
    }

    // --- Export to Excel (Apache POI)
    private void exportToExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("expenses.xlsx"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                Sheet sheet = wb.createSheet("Expenses");
                Row header = sheet.createRow(0);
                for (int i = 0; i < tableModel.getColumnCount(); i++) header.createCell(i).setCellValue(tableModel.getColumnName(i));
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        Object val = tableModel.getValueAt(r, c);
                        if (val instanceof Number) row.createCell(c).setCellValue(((Number) val).doubleValue());
                        else row.createCell(c).setCellValue(val == null ? "" : val.toString());
                    }
                }
                wb.write(fos);
                JOptionPane.showMessageDialog(this, "Exported to Excel!");
            } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage()); }
        }
    }

    // --- Export to PDF (iText)
    private void exportToPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("expenses.pdf"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                PdfWriter writer = new PdfWriter(fos);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf);
                Table t = new Table(tableModel.getColumnCount());
                for (int i = 0; i < tableModel.getColumnCount(); i++) t.addHeaderCell(new Cell().add(new Paragraph(tableModel.getColumnName(i))));
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        Object val = tableModel.getValueAt(r, c);
                        t.addCell(new Cell().add(new Paragraph(val == null ? "" : val.toString())));
                    }
                }
                doc.add(t);
                doc.close();
                JOptionPane.showMessageDialog(this, "PDF exported!");
            } catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "PDF export failed: " + ex.getMessage()); }
        }
    }

    // --- Charts using JFreeChart
    private void refreshCharts() {
        chartsPanel.removeAll();

        // Pie chart: category totals
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        try (ResultSet rs = dao.getCategoryTotals()) {
            while (rs.next()) {
                pieDataset.setValue(rs.getString("category"), rs.getDouble("total"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        JFreeChart pieChart = ChartFactory.createPieChart("Spending by Category", pieDataset, true, true, false);
        ChartPanel piePanel = new ChartPanel(pieChart);
        chartsPanel.add(piePanel);

        // Bar chart: monthly totals
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        try (ResultSet rs = dao.getMonthlyTotals()) {
            while (rs.next()) {
                barDataset.addValue(rs.getDouble("total"), "Amount", rs.getString("month"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        JFreeChart barChart = ChartFactory.createBarChart("Monthly Expenses", "Month", "Amount (₹)", barDataset);
        ChartPanel barPanel = new ChartPanel(barChart);
        chartsPanel.add(barPanel);

        chartsPanel.revalidate();
        chartsPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExpenseTrackerUI().setVisible(true));
    }
}

