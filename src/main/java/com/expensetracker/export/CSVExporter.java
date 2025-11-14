package com.expensetracker.export;

import com.expensetracker.model.Transaction;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVExporter {

    public static boolean exportTransactions(String path, List<Transaction> list) {
        try (FileWriter fw = new FileWriter(path)) {

            // Updated header (matches new model: description & date)
            fw.write("id,userId,amount,category,type,description,date\n");

            for (Transaction t : list) {

                // Escape commas inside text fields
                String safeCategory = t.getCategory().replace(",", ";");
                String safeDesc = t.getDescription().replace(",", ";");

                fw.write(String.format(
                        "%d,%d,%.2f,%s,%s,%s,%s\n",
                        t.getId(),
                        t.getUserId(),
                        t.getAmount(),
                        safeCategory,
                        t.getType(),
                        safeDesc,
                        t.getDate()     // now using string date
                ));
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
