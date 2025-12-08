package com.profitsoft.application.utils;

import com.profitsoft.application.entities.Book;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.List;

public class CsvExporter {
    public static void writeBooksToCsv(List<Book> books, OutputStream output) throws IOException {
        try (Writer writer = new OutputStreamWriter(output);
             CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "ID", "Title", "Author", "Year Published", "Genres"
             ))) {
            for (Book b : books) {
                csv.printRecord(
                        b.getId(),
                        b.getTitle(),
                        b.getAuthor() != null ? b.getAuthor().getName() : "Unknown",
                        b.getYearPublished(),
                        b.getGenres() != null ? String.join("|", b.getGenres()) : ""
                );
            }
            csv.flush();
        }
    }
}
