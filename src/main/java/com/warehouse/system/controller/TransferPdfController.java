package com.warehouse.system.controller;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.warehouse.system.entity.Item;
import com.warehouse.system.entity.TransferRequest;
import com.warehouse.system.repository.TransferRequestRepository;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Controller
public class TransferPdfController {

    private final TransferRequestRepository transferRequestRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TransferPdfController(TransferRequestRepository transferRequestRepository) {
        this.transferRequestRepository = transferRequestRepository;
    }

    @GetMapping("/ui/transfer-requests/pdf")
    public void generateBulkPdf(@RequestParam("ids") String ids,
                                HttpSession session,
                                HttpServletResponse response) throws Exception {

        if (!AuthController.isLoggedIn(session)) {
            response.sendRedirect("/ui/login");
            return;
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .toList();

        List<TransferRequest> requests = transferRequestRepository.findAllById(idList);
        if (requests.isEmpty()) return;

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=transfer_summary.pdf");

        try (OutputStream os = response.getOutputStream()) {
            buildFullBulkPdf(os, requests);
        }
    }

    private void buildFullBulkPdf(OutputStream os, List<TransferRequest> requests) throws Exception {
        // דף לרוחב כדי שכל העמודות ייכנסו יפה (Product, SN, Catalog, Warehouse, Location)
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();

        Font titleF = hebrew(22, Font.BOLD);
        Font headerF = hebrew(12, Font.BOLD);
        Font normalF = hebrew(11, Font.NORMAL);
        Font smallF = hebrew(9, Font.NORMAL);

        // --- Header Section ---
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        headerTable.setWidths(new float[]{75, 25});

        PdfPCell tCell = rtlCell("דיווח ריכוז העברת ציוד", titleF, Element.ALIGN_CENTER, false);
        tCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(tCell);

        PdfPCell dCell = rtlCell("הופק בתאריך: " + LocalDateTime.now().format(FMT), smallF, Element.ALIGN_LEFT, false);
        dCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(dCell);
        doc.add(headerTable);

        addSpacer(doc, 15);

        // --- Parties Info (From/To) ---
        TransferRequest first = requests.get(0);
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        parties.addCell(block("מוסר:", first.getFromUser().getFullName() + " (" + first.getFromUser().getPersonalNumber() + ")", headerF, normalF));
        parties.addCell(block("מקבל:", first.getToUser().getFullName() + " (" + first.getToUser().getPersonalNumber() + ")", headerF, normalF));
        doc.add(parties);

        addSpacer(doc, 20);

        // --- Items Table (The core of the bulk) ---
        // עמודות: מוצר, מק"ט, מחסן, מספר סיריאלי, מיקום, סטטוס
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.setWidths(new float[]{25, 15, 15, 20, 15, 10});

        table.addCell(rtlCell("מוצר", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("קטלוג/מק\"ט", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("מחסן", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("מספר סיריאלי", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("מיקום", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("כמות", headerF, Element.ALIGN_CENTER, true));

        for (TransferRequest tr : requests) {
            Item item = tr.getItem();
            table.addCell(rtlCell(item.getItemType().getName(), normalF, Element.ALIGN_RIGHT, false));
            table.addCell(rtlCell(item.getItemType().getCatalogNumber(), normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(item.getWarehouse().getName(), normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(item.getSerialNumber() != null ? item.getSerialNumber() : "Bulk", normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(item.getLocation(), normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell("1", normalF, Element.ALIGN_CENTER, false));
        }
        doc.add(table);

        addSpacer(doc, 30);

        // --- Signatures ---
        PdfPTable sigs = new PdfPTable(2);
        sigs.setWidthPercentage(100);
        sigs.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        sigs.addCell(signatureBox("חתימת מוסר:", normalF));
        sigs.addCell(signatureBox("חתימת מקבל:", normalF));
        doc.add(sigs);

        doc.close();
    }

    // --- Helper Methods (לשמירה על ה-RTL והעברית) ---

    private Font hebrew(float size, int style) throws Exception {
        ClassPathResource res = new ClassPathResource("fonts/NotoSansHebrew-Regular.ttf");
        BaseFont bf = BaseFont.createFont("fonts/NotoSansHebrew-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, res.getInputStream().readAllBytes(), null);
        return new Font(bf, size, style);
    }

    private PdfPCell rtlCell(String value, Font font, int align, boolean header) {
        PdfPCell c = new PdfPCell(new Phrase(value != null ? value : "—", font));
        c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(8f);
        if (header) c.setBackgroundColor(new Color(230, 230, 230));
        return c;
    }

    private PdfPCell block(String title, String value, Font titleFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setPadding(10f);
        Paragraph p1 = new Paragraph(title, titleFont);
        Paragraph p2 = new Paragraph(value, valueFont);
        cell.addElement(p1);
        cell.addElement(p2);
        return cell;
    }

    private PdfPCell signatureBox(String label, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setPadding(20f);
        cell.addElement(new Paragraph(label + " ____________________", font));
        return cell;
    }

    private void addSpacer(Document doc, float height) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        doc.add(p);
    }
}