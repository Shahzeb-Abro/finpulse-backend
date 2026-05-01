package com.finpulse.util.pdf;

import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PdfHelper — generates branded PDFs matching FinPulse's email aesthetic.
 * <p>
 * Design language:
 * - Warm beige page background (#f8f4f0)
 * - Dark hero header (#201f24) with white title + green accent label
 * - White content cards with rounded corners
 * - Green accent (#277c78) for highlights
 * - Public Sans-style typography (uses default with similar weights)
 * <p>
 * Usage:
 * ByteArrayOutputStream out = PdfHelper.builder()
 * .heroLabel("Transaction Report")
 * .heroTitle("April 2026")
 * .heroSubtitle("Your spending and income overview")
 * .summary(summaryRow)
 * .table(tableHeaders, tableRows)
 * .footer("Generated on 29 Apr 2026 · FinPulse")
 * .build();
 */
public class PDFHelperUtils {

    // ─── Color palette (matches email template) ──────────────────
    public static final Color BG_BEIGE = new DeviceRgb(248, 244, 240);  // #f8f4f0
    public static final Color DARK = new DeviceRgb(32, 31, 36);     // #201f24
    public static final Color WHITE = new DeviceRgb(255, 255, 255);
    public static final Color GREY_500 = new DeviceRgb(105, 104, 104);  // #696868
    public static final Color GREY_300 = new DeviceRgb(179, 179, 179);  // #b3b3b3
    public static final Color GREY_100 = new DeviceRgb(242, 242, 242);  // #f2f2f2
    public static final Color GREEN = new DeviceRgb(39, 124, 120);   // #277c78
    public static final Color RED = new DeviceRgb(201, 71, 54);    // destructive
    public static final Color GREY_BEIGE = new DeviceRgb(151, 144, 136);  // #979088

     // ─── With these ───────────────────────────────────────────────
    static {
        FontProgramFactory.registerFont(
                PDFHelperUtils.class.getClassLoader()
                        .getResource("fonts/PublicSans-Regular.ttf").getPath(),
                "PublicSans-Regular"
        );
        FontProgramFactory.registerFont(
                PDFHelperUtils.class.getClassLoader()
                        .getResource("fonts/PublicSans-Bold.ttf").getPath(),
                "PublicSans-Bold"
        );

         FontProgramFactory.registerFont(
                 PDFHelperUtils.class.getClassLoader()
                         .getResource("fonts/NotoSans-Regular.ttf").getPath(),
                 "NotoSans-Regular"
         );

         FontProgramFactory.registerFont(
                 PDFHelperUtils.class.getClassLoader()
                         .getResource("fonts/NotoSans-Bold.ttf").getPath(),
                 "NotoSans-Bold"
         );


    }

    public static PdfFont regular() {
        try {
            return PdfFontFactory.createRegisteredFont(
                    "PublicSans-Regular", "Identity-H",
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create regular font", e);
        }
    }

    public static PdfFont notoRegular() {
        try {
            return PdfFontFactory.createRegisteredFont(
                    "NotoSans-Regular", "Identity-H",
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create regular font", e);
        }
    }

    public static PdfFont bold() {
        try {
            return PdfFontFactory.createRegisteredFont(
                    "PublicSans-Bold", "Identity-H",
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bold font", e);
        }
    }

    public static PdfFont notoBold() {
        try {
            return PdfFontFactory.createRegisteredFont(
                    "NotoSans-Bold", "Identity-H",
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bold font", e);
        }
    }

    // ─── Hero header ─────────────────────────────────────────────
    public static Div heroHeader(String label, String title, String subtitle) {
        Div hero = new Div()
                .setBackgroundColor(DARK)
                .setPadding(24)
                .setBorderRadius(new BorderRadius(8))
                .setMarginBottom(16);

        // Small uppercase label (green accent)
        Paragraph labelText = new Paragraph(label.toUpperCase())
                .setFontSize(9)
                .setFont(bold())
                .setFontColor(GREEN)
                .setMarginBottom(4)
                .setCharacterSpacing(1.2f);

        // Big white title
        Paragraph titleText = new Paragraph(title)
                .setFontSize(18)
                .setFont(bold())
                .setFontColor(WHITE)
                .setMarginBottom(subtitle != null ? 6 : 0)
                .setMultipliedLeading(1.15f);

        hero.add(labelText).add(titleText);

        // Optional subtitle
        if (subtitle != null) {
            hero.add(new Paragraph(subtitle)
                    .setFontSize(10)
                    .setFont(regular())
                    .setFontColor(GREY_300)
                    .setMultipliedLeading(1.3f));
        }

        return hero;
    }

    // ─── Content card ────────────────────────────────────────────
    public static Div contentCard() {
        return new Div()
                .setBackgroundColor(WHITE)
                .setPadding(20)
                .setBorderRadius(new BorderRadius(12))
                .setMarginBottom(12);
    }

    // ─── Summary callout (highlighted box with key stats) ────────
    public static Div summaryCallout(String label, String value, Color valueColor) {
        Div callout = new Div()
                .setBackgroundColor(BG_BEIGE)
                .setPadding(16)
                .setBorderRadius(new BorderRadius(8))
                .setBorderLeft(new SolidBorder(GREEN, 3));

        callout.add(new Paragraph(label)
                .setFontSize(11)
                        .setFont(bold())
                .setFontColor(GREY_500)
                .setMarginBottom(4));

        callout.add(new Paragraph(value)
                .setFontSize(22)
                .setFont(bold())
                .setFontColor(valueColor)
                .setMargin(0));

        return callout;
    }

    // ─── 3-column summary row (income / expense / net) ───────────
    public static Table summaryRow(
            String incomeLabel,   String incomeSymbol,  String incomeAmount,
            String expenseLabel,  String expenseSymbol, String expenseAmount,
            String netLabel,      String netSymbol,     String netAmount,
            boolean netIsPositive
    ) {
        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(0);

        summary.addCell(summaryCell(incomeLabel,  "+", incomeSymbol,  incomeAmount,  GREEN));
        summary.addCell(summaryCell(expenseLabel, "-", expenseSymbol, expenseAmount, RED));
        summary.addCell(summaryCell(netLabel,
                netIsPositive ? "+" : "-", netSymbol, netAmount,
                netIsPositive ? GREEN : RED));

        return summary;
    }

    private static Cell summaryCell(
            String label, String sign, String symbol, String amount, Color valueColor
    ) {
        Cell cell = new Cell()
                .setBackgroundColor(BG_BEIGE)
                .setPadding(12)
                .setBorder(Border.NO_BORDER)
                .setMargin(3);

        cell.add(new Paragraph(label)
                .setFontSize(9)
                .setFont(regular())
                .setFontColor(GREY_500)
                .setMarginBottom(4)
                .setCharacterSpacing(0.4f));

        // Mixed font paragraph — sign + symbol in Noto, amount in Public Sans Bold
        Paragraph valueParagraph = new Paragraph()
                .add(new Text(sign).setFont(notoBold()).setFontColor(valueColor))
                .add(new Text(symbol + " ").setFont(notoBold()).setFontColor(valueColor))
                .add(new Text(amount).setFont(bold()).setFontColor(valueColor))
                .setFontSize(15)
                .setMargin(0);

        cell.add(valueParagraph);
        return cell;
    }

    // ─── Section heading ─────────────────────────────────────────
    public static Paragraph sectionHeading(String text) {
        return new Paragraph(text)
                .setFontSize(12)
                .setFont(bold())
                .setFontColor(DARK)
                .setMarginTop(0)
                .setMarginBottom(8);
    }

    // ─── Branded table ───────────────────────────────────────────
    public static Table table(float[] columnWidths, String[] headers) {
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(8);

        for (String header : headers) {
            table.addHeaderCell(headerCell(header));
        }

        return table;
    }

    private static Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFontSize(9)
                        .setFont(bold())
                        .setFontColor(GREY_500)
                        .setCharacterSpacing(0.4f)
                        .setMargin(0))
                .setBackgroundColor(BG_BEIGE)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GREY_100, 1));
    }

    public static Cell bodyCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—")
                        .setFontSize(10)
                        .setFont(regular())
                        .setFontColor(DARK)
                        .setMargin(0))
                .setPadding(8)

                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GREY_100, 0.5f));
    }

    public static Cell amountCell(String symbol, String amount, boolean isIncome) {
        Color color = isIncome ? GREEN : RED;

        Paragraph p = new Paragraph()
                .add(new Text(isIncome ? "+" : "-")).setFont(notoRegular()).setFontColor(color)
                .add(new Text(symbol + " ").setFont(notoBold()).setFontColor(color))
                .add(new Text(amount).setFont(bold()).setFontColor(color))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMargin(0);

        return new Cell()
                .add(p)
                .setFontSize(10)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GREY_100, 0.5f));
    }

    public static Cell mutedCell(String text) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—")
                        .setFontSize(9)
                        .setFont(regular())
                        .setFontColor(GREY_500)
                        .setMargin(0))
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GREY_100, 0.5f));
    }

    // ─── Brand mark (top-left "finance" wordmark) ────────────────
    public static Paragraph brandMark() {
        return new Paragraph("finance")
                .setFontSize(14)
                .setFont(bold())
                .setFontColor(DARK)
                .setCharacterSpacing(-0.3f)
                .setMarginBottom(12);
    }

    // ─── Footer ──────────────────────────────────────────────────
    public static Paragraph footer(String text) {
        return new Paragraph(text)
                .setFontSize(9)
                .setFont(regular())
                .setFontColor(GREY_BEIGE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(12);
    }

    // ─── Document factory — sets up page size, margins, bg ───────
    public static Document createDocument(ByteArrayOutputStream out) {
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(28, 32, 28, 32);

        // Beige page background — drawn on every page via event handler
        pdf.addEventHandler(
                PdfDocumentEvent.START_PAGE,
                new BackgroundEventHandler(BG_BEIGE)
        );

        return doc;
    }

    // ─── Format helpers ──────────────────────────────────────────
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    public static String formatAmount(BigDecimal amount, boolean isIncome, String currencySymbol) {
       return (isIncome ? "+" : "-") + currencySymbol + amount.toPlainString();
    }
}