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
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        response.setHeader("Content-Disposition", "inline; filename=form_1008.pdf");

        try (OutputStream os = response.getOutputStream()) {
            buildFullBulkPdf(os, requests);
        }
    }

    private void buildFullBulkPdf(OutputStream os, List<TransferRequest> requests) throws Exception {
        // הגדרת מסמך A4 עם שוליים תקניים
        Document doc = new Document(PageSize.A4, 25, 25, 25, 25);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();

        Font titleF = hebrew(24, Font.BOLD);
        Font headerF = hebrew(11, Font.BOLD);
        Font normalF = hebrew(11, Font.NORMAL);
        Font smallF = hebrew(9, Font.NORMAL);

        // --- כותרת ראשית ---
        PdfPTable titleTab = new PdfPTable(1);
        titleTab.setWidthPercentage(100);
        titleTab.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        titleTab.addCell(noBorderCell("שובר השאלת אפסניה", titleF, Element.ALIGN_CENTER));
        doc.add(titleTab);

        addSpacer(doc, 20);

        // --- טבלת פרטי העברה (מאת/אל/מיקום) ---
        TransferRequest first = requests.get(0);
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        infoTable.setWidths(new float[]{35, 30, 35});

        // ימין: מאת (המוסר)
        infoTable.addCell(rtlCell("מאת (המוסר):\n" + first.getFromUser().getFullName(), smallF, Element.ALIGN_RIGHT, false));

        // מרכז: מיקום (סדר מילים הפוך לטובת תצוגה נכונה ב-PDF)
        infoTable.addCell(rtlCell("מיקום:בהד 20 ", smallF, Element.ALIGN_CENTER, true));

        // שמאל: אל (המקבל)
        infoTable.addCell(rtlCell("אל (המקבל):\n" + first.getToUser().getFullName(), smallF, Element.ALIGN_RIGHT, false));
        doc.add(infoTable);

        addSpacer(doc, 10);

        // --- טבלת פריטים ---
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.setWidths(new float[]{15, 45, 10, 10, 20});

        table.addCell(rtlCell("מסט\"ב", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("שם פריט", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("יח' רישום", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("כמות", headerF, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("סיריאל / הערות", headerF, Element.ALIGN_CENTER, true));

        for (TransferRequest tr : requests) {
            Item item = tr.getItem();
            table.addCell(rtlCell(item.getItemType().getCatalogNumber(), normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(item.getItemType().getName(), normalF, Element.ALIGN_RIGHT, false));
            table.addCell(rtlCell("יח'", normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell("1", normalF, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(item.getSerialNumber() != null ? item.getSerialNumber() : "—", normalF, Element.ALIGN_CENTER, false));
        }

        // השלמת שורות ריקות למראה טופס 1008 מלא
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 5; j++) table.addCell(rtlCell(" ", normalF, Element.ALIGN_CENTER, false));
        }
        doc.add(table);

        addSpacer(doc, 30);

        // --- חתימות ---
        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(100);
        footer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        footer.addCell(sigBox("פרטי המוסר", first.getFromUser(), smallF, headerF));
        footer.addCell(sigBox("פרטי המקבל", first.getToUser(), smallF, headerF));
        doc.add(footer);

        doc.close();
    }

    private Font hebrew(float size, int style) throws Exception {
        ClassPathResource res = new ClassPathResource("fonts/NotoSansHebrew-Regular.ttf");
        BaseFont bf = BaseFont.createFont("fonts/NotoSansHebrew-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, res.getInputStream().readAllBytes(), null);
        return new Font(bf, size, style);
    }

    private PdfPCell rtlCell(String text, Font font, int align, boolean gray) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        if (gray) cell.setBackgroundColor(new Color(245, 245, 245));
        return cell;
    }

    private PdfPCell noBorderCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell sigBox(String title, com.warehouse.system.entity.User user, Font sF, Font hF) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setPadding(10f);
        cell.addElement(new Paragraph(title + ":", hF));
        cell.addElement(new Paragraph("תאריך: " + LocalDateTime.now().format(DATE_FMT), sF));
        cell.addElement(new Paragraph("שם: " + user.getFullName(), sF));
        cell.addElement(new Paragraph("מ.א: " + user.getPersonalNumber(), sF));
        cell.addElement(new Paragraph("חתימה: ________________", sF));
        return cell;
    }

    private void addSpacer(Document doc, float height) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        doc.add(p);
    }
}