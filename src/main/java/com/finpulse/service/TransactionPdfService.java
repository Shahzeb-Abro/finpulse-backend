package com.finpulse.service;

import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import com.finpulse.enums.TransactionType;
import com.finpulse.util.pdf.PDFHelperUtils;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionPdfService {

    public byte[] generateTransactionPdf(
            List<Transaction> transactions,
            User user,
            LocalDate from,
            LocalDate to
    ) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = PDFHelperUtils.createDocument(out);

        // ── Brand mark ──
        doc.add(PDFHelperUtils.brandMark());

        // ── Hero header ──
        String dateRange = from.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                " — " +
                to.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        doc.add(PDFHelperUtils.heroHeader(
                "Transaction Report",
                user.getFullName().split(" ")[0] + "'s activity",  // Single line — "Shahzeb's activity"
                dateRange
        ));

        // ── Compute summary ──
        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = totalIncome.subtract(totalExpense);

        // ── Summary card with 3-col row ──
        Div summaryCard = PDFHelperUtils.contentCard();
        summaryCard.add(PDFHelperUtils.sectionHeading("Summary"));
        summaryCard.add(PDFHelperUtils.summaryRow(
                "Total Income",   "+$" + totalIncome.toPlainString(),
                "Total Expenses", "-$" + totalExpense.toPlainString(),
                "Net",            (net.signum() >= 0 ? "+$" : "-$") + net.abs().toPlainString(),
                net.signum() >= 0
        ));
        doc.add(summaryCard);

        // ── Transactions table card ──
        Div transactionsCard = PDFHelperUtils.contentCard();
        transactionsCard.add(PDFHelperUtils.sectionHeading(
                "All Transactions (" + transactions.size() + ")"
        ));

        if (transactions.isEmpty()) {
            transactionsCard.add(PDFHelperUtils.bodyCell("No transactions in this period."));
        } else {
            Table table = PDFHelperUtils.table(
                    new float[]{3.5f, 2f, 2f, 2f},
                    new String[]{"Description", "Category", "Date", "Amount"}
            );

            for (Transaction t : transactions) {
                boolean isIncome = t.getTransactionType() == TransactionType.INCOME;

                table.addCell(PDFHelperUtils.bodyCell(t.getDescription()));
                table.addCell(PDFHelperUtils.mutedCell(
                        t.getCategory() != null
                                ? t.getCategory().getVisibleValue()
                                : "—"
                ));
                table.addCell(PDFHelperUtils.mutedCell(
                        PDFHelperUtils.formatDate(t.getTransactionDate())
                ));
                table.addCell(PDFHelperUtils.amountCell(
                        PDFHelperUtils.formatAmount(t.getAmount(), isIncome),
                        isIncome
                ));
            }

            transactionsCard.add(table);
        }

        doc.add(transactionsCard);

        // ── Footer ──
        doc.add(PDFHelperUtils.footer(
                "Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                        " · finance"
        ));

        doc.close();
        return out.toByteArray();
    }
}