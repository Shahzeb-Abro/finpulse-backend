package com.finpulse.util.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

/**
 * Paints a solid background color on every page.
 * Used to give the PDF a beige page color matching the email template.
 */
public class BackgroundEventHandler implements IEventHandler {

    private final Color color;

    public BackgroundEventHandler(Color color) {
        this.color = color;
    }

    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();

        PdfCanvas canvas = new PdfCanvas(
                page.newContentStreamBefore(),
                page.getResources(),
                pdfDoc
        );

        canvas.saveState()
                .setFillColor(color)
                .rectangle(pageSize.getLeft(), pageSize.getBottom(),
                        pageSize.getWidth(), pageSize.getHeight())
                .fill()
                .restoreState();
    }
}