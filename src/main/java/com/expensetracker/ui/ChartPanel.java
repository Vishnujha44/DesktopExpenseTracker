package com.expensetracker.ui;

import com.expensetracker.dao.TransactionDAO;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ChartPanel - replacement that provides:
 * - public ChartPanel()
 * - public void setUserId(int)
 * - public void setDAO(TransactionDAO)
 * - public boolean exportAsImage(File)
 *
 * Works with TransactionDAO methods returning List<Object[]>
 */
public class ChartPanel extends JPanel {
    private int userId = -1;
    private TransactionDAO dao;

    public ChartPanel() {
        setBackground(new Color(34, 34, 34));
        setPreferredSize(new Dimension(400, 300));
    }

    public void setUserId(int id) {
        this.userId = id;
    }

    public void setDAO(TransactionDAO dao) {
        this.dao = dao;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();

        int pieSize = Math.min(w / 2 - 30, h - 60);
        int pieX = 15;
        int pieY = 15;
        int barX = pieX + pieSize + 20;
        int barY = 20;
        int barW = w - barX - 20;
        int barH = h - 80;

        List<Object[]> catRows = new ArrayList<>();
        List<Object[]> monthRows = new ArrayList<>();
        try {
            if (dao != null && userId != -1) {
                catRows = dao.getCategoryTotals(userId);
                monthRows = dao.getMonthlyTotals(userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (catRows == null || catRows.isEmpty()) {
            catRows = List.of(
                    new Object[]{"Food", 1200.0},
                    new Object[]{"Transport", 800.0},
                    new Object[]{"Bills", 300.0}
            );
        }

        if (monthRows == null || monthRows.isEmpty()) {
            monthRows = List.of(
                    new Object[]{"2025-01", 400.0},
                    new Object[]{"2025-02", 600.0},
                    new Object[]{"2025-03", 900.0},
                    new Object[]{"2025-04", 300.0}
            );
        }

        double total = 0.0;
        for (Object[] r : catRows) {
            Number n = (Number) r[1];
            total += (n == null ? 0.0 : n.doubleValue());
        }
        double angleStart = 0.0;
        for (int i = 0; i < catRows.size(); i++) {
            Object[] r = catRows.get(i);
            double value = ((Number) r[1]).doubleValue();
            double angle = (total == 0) ? 0 : (value / total) * 360.0;
            g2.setColor(getColor(i));
            g2.fillArc(pieX, pieY, pieSize, pieSize, (int) Math.round(angleStart), (int) Math.round(angle));
            angleStart += angle;
        }
        g2.setColor(Color.WHITE);
        g2.drawString("Expenses by Category", pieX, pieY + pieSize + 18);

        int legendX = pieX;
        int legendY = pieY + pieSize + 30;
        for (int i = 0; i < catRows.size(); i++) {
            g2.setColor(getColor(i));
            g2.fillRect(legendX, legendY + i * 18, 12, 12);
            g2.setColor(Color.WHITE);
            String label = catRows.get(i)[0] == null ? "Uncategorized" : catRows.get(i)[0].toString();
            g2.drawString(label, legendX + 18, legendY + i * 18 + 12);
        }

        int n = monthRows.size();
        if (n > 0) {
            double max = 0.0;
            for (Object[] r : monthRows) {
                double v = ((Number) r[1]).doubleValue();
                if (v > max) max = v;
            }
            int bw = Math.max(20, barW / (n * 2));
            int gap = bw / 2;
            for (int i = 0; i < n; i++) {
                int x = barX + i * (bw + gap);
                double v = ((Number) monthRows.get(i)[1]).doubleValue();
                int bh = (int) ((max == 0) ? 0 : (v / max) * (barH - 40));
                int y = barY + (barH - bh);
                g2.setColor(getColor(i));
                g2.fillRect(x, y, bw, bh);
                g2.setColor(Color.WHITE);
                String m = monthRows.get(i)[0] == null ? "" : monthRows.get(i)[0].toString();
                g2.drawString(m, x, barY + barH + 15);
            }
            g2.setColor(Color.WHITE);
            g2.drawString("Monthly Expense Trend", barX, barY + barH + 35);
        }

        g2.dispose();
    }

    private Color getColor(int i) {
        Color[] palette = new Color[]{
                new Color(0, 150, 136),
                new Color(3, 169, 244),
                new Color(255, 193, 7),
                new Color(244, 67, 54),
                new Color(156, 39, 176),
                new Color(63, 81, 181)
        };
        return palette[i % palette.length];
    }

    public boolean exportAsImage(File outFile) {
        try {
            int w = Math.max(getWidth(), 800);
            int h = Math.max(getHeight(), 600);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            this.setSize(w, h);
            this.paint(g2);
            g2.dispose();
            ImageIO.write(img, "png", outFile);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}


