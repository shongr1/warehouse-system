package com.warehouse.system.controller;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.warehouse.system.entity.Item;
import com.warehouse.system.entity.TransferRequest;
import com.warehouse.system.entity.TransferStatus;
import com.warehouse.system.repository.ItemRepository;
import com.warehouse.system.repository.TransferRequestRepository;
import com.warehouse.system.repository.UserRepository;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.awt.*;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class TransferPdfController {

    private final TransferRequestRepository transferRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public TransferPdfController(TransferRequestRepository transferRequestRepository,
                                 ItemRepository itemRepository,
                                 UserRepository userRepository) {
        this.transferRequestRepository = transferRequestRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    // PDF לבקשה אחת (כפתור "PDF" בכל שורה)
    @GetMapping("/ui/transfer-requests/{id}/pdf")
    public void oneRequestPdf(@PathVariable Long id,
                              HttpSession session,
                              HttpServletResponse response) throws Exception {

        if (!AuthController.isLoggedIn(session)) {
            response.sendRedirect("/ui/login");
            return;
        }

        String pn = AuthController.currentPn(session);

        var me = userRepository.findByPersonalNumber(pn.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TransferRequest tr = transferRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TransferRequest not found: " + id));

        // רק המקבל יכול לצפות ב-PDF של הבקשה שלו
        if (!me.getId().equals(tr.getToUserId())) {
            throw new RuntimeException("Forbidden: not your request");
        }

        Item item = itemRepository.findById(tr.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + tr.getItemId()));

        var fromUser = userRepository.findById(tr.getFromUserId())
                .orElseThrow(() -> new RuntimeException("From user not found"));
        var toUser = userRepository.findById(tr.getToUserId())
                .orElseThrow(() -> new RuntimeException("To user not found"));

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=transfer_request_" + tr.getId() + ".pdf");

        try (OutputStream os = response.getOutputStream()) {
            buildOneRequestPdf(os, tr, item, fromUser.getFullName(), fromUser.getPersonalNumber(),
                    toUser.getFullName(), toUser.getPersonalNumber());
        }
    }

    // PDF לרשימת כל הבקשות הנכנסות PENDING (כפתור Export PDF למעלה)
    @GetMapping("/ui/my-transfer-requests/pdf")
    public void myIncomingPdf(HttpSession session, HttpServletResponse response) throws Exception {
        if (!AuthController.isLoggedIn(session)) {
            response.sendRedirect("/ui/login");
            return;
        }

        String pn = AuthController.currentPn(session);

        var me = userRepository.findByPersonalNumber(pn.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TransferRequest> requests =
                transferRequestRepository.findAllByToUserIdAndStatus(me.getId(), TransferStatus.PENDING);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=incoming_transfers.pdf");

        try (OutputStream os = response.getOutputStream()) {
            buildIncomingListPdf(os, requests, me.getFullName(), me.getPersonalNumber());
        }
    }

    // =========================
    // PDF BUILDERS (HIGH QUALITY)
    // =========================

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Font hebrew(float size, int style) throws Exception {
        // ✅ שים פונט כאן: src/main/resources/fonts/NotoSansHebrew-Regular.ttf
        ClassPathResource res = new ClassPathResource("fonts/NotoSansHebrew-Regular.ttf");
        byte[] bytes = res.getInputStream().readAllBytes();

        BaseFont bf = BaseFont.createFont(
                "NotoSansHebrew-Regular.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED,
                true,
                bytes,
                null
        );

        Font f = new Font(bf, size, style);
        f.setColor(Color.BLACK);
        return f;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String fmt(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(FMT);
    }

    /** מייצר תא RTL יציב (לא מתהפך גם עם מספרים/אנגלית) */
    private PdfPCell rtlCell(String value, Font font, int align, boolean header) {
        PdfPCell c = new PdfPCell(new Phrase(safe(value), font));
        c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(10f);
        c.setBorderWidth(1f);
        if (header) {
            c.setBackgroundColor(new Color(245, 245, 245));
        }
        return c;
    }

    /** שורה "Label : Value" הכי יציבה (לא מתהפך) */
    private PdfPTable keyValueRow(String label, String value, Font labelFont, Font valueFont) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        try {
            t.setWidths(new float[]{25, 75}); // label קטן, value גדול
        } catch (Exception ignored) {}

        t.addCell(rtlCell(label, labelFont, Element.ALIGN_RIGHT, true));
        t.addCell(rtlCell(value, valueFont, Element.ALIGN_RIGHT, false));
        return t;
    }

    private void addSpacer(Document doc, float height) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        doc.add(p);
    }

    private void buildOneRequestPdf(OutputStream os,
                                    TransferRequest tr,
                                    Item item,
                                    String fromName, String fromPn,
                                    String toName, String toPn) throws Exception {

        // ✅ נראה כמו הטופס שצילמת (רחב)
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();

        Font title = hebrew(22, Font.BOLD);
        Font h = hebrew(12, Font.BOLD);
        Font n = hebrew(12, Font.NORMAL);
        Font small = hebrew(10, Font.NORMAL);

        // ===== Header =====
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        header.setWidths(new float[]{80, 20}); // כותרת רחבה, תאריך צר

        PdfPCell titleCell = rtlCell("דיווח על העברת ציוד", title, Element.ALIGN_CENTER, false);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(0);

        PdfPCell dateCell = rtlCell(
                "תאריך יצירה: " + fmt(LocalDateTime.now()),
                small,
                Element.ALIGN_RIGHT,   // ✅ היה LEFT — לשנות ל־RIGHT
                false
        );
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setPadding(0);


        header.addCell(titleCell);
        header.addCell(dateCell);

        doc.add(header);
        addSpacer(doc, 8);
        addSpacer(doc, 8);

        // ===== Top info table (4 blocks like your screenshot) =====
        PdfPTable top = new PdfPTable(4);
        top.setWidthPercentage(100);
        top.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        top.setSpacingBefore(6);
        top.setSpacingAfter(10);
        top.setWidths(new float[]{25, 25, 25, 25});

        // כל בלוק: כותרת (מודגש) ושורה מתחת
        top.addCell(block("מספר בקשה:", String.valueOf(tr.getId()), h, n));
        top.addCell(block("סטטוס:", String.valueOf(tr.getStatus()), h, n));
        top.addCell(block("תאריך יצירה:", fmt(tr.getCreatedAt()), h, n));
        top.addCell(block("הערה:", safe(tr.getNote()), h, n));

        doc.add(top);

        // ===== From/To =====
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        parties.setSpacingBefore(6);
        parties.setSpacingAfter(10);
        parties.setWidths(new float[]{50, 50});

        parties.addCell(block("מוסר:", safe(fromName) + " (" + safe(fromPn) + ")", h, n));
        parties.addCell(block("מקבל:", safe(toName) + " (" + safe(toPn) + ")", h, n));

        doc.add(parties);

        // ===== Item table (like your screenshot: Product/Catalog/Warehouse/Serial/Location) =====
        PdfPTable it = new PdfPTable(5);
        it.setWidthPercentage(100);
        it.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        it.setSpacingBefore(6);
        it.setSpacingAfter(16);
        it.setWidths(new float[]{28, 16, 22, 18, 16});

        it.addCell(rtlCell("מוצר", h, Element.ALIGN_CENTER, true));
        it.addCell(rtlCell("קטלוג", h, Element.ALIGN_CENTER, true));
        it.addCell(rtlCell("מחסן", h, Element.ALIGN_CENTER, true));
        it.addCell(rtlCell("סיריאל", h, Element.ALIGN_CENTER, true));
        it.addCell(rtlCell("מיקום", h, Element.ALIGN_CENTER, true));

        String product = (item.getItemType() != null) ? safe(item.getItemType().getName()) : "—";
        String catalog = (item.getItemType() != null) ? safe(item.getItemType().getCatalogNumber()) : "—";
        String wh = (item.getWarehouse() != null) ? safe(item.getWarehouse().getName()) : "—";
        String serial = safe(item.getSerialNumber());
        String location = safe(item.getLocation());

        it.addCell(rtlCell(product, n, Element.ALIGN_CENTER, false));
        it.addCell(rtlCell(catalog, n, Element.ALIGN_CENTER, false));
        it.addCell(rtlCell(wh, n, Element.ALIGN_CENTER, false));
        it.addCell(rtlCell(serial, n, Element.ALIGN_CENTER, false));
        it.addCell(rtlCell(location, n, Element.ALIGN_CENTER, false));

        doc.add(it);

        // ===== Signatures area =====
        PdfPTable sig = new PdfPTable(2);
        sig.setWidthPercentage(100);
        sig.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        sig.setSpacingBefore(8);
        sig.setWidths(new float[]{50, 50});

        sig.addCell(signatureBox("חתימת מוסר:", n));
        sig.addCell(signatureBox("חתימת מקבל:", n));

        doc.add(sig);

        addSpacer(doc, 10);

        PdfPTable dateRow = new PdfPTable(1);
        dateRow.setWidthPercentage(100);
        dateRow.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        dateRow.addCell(signatureBox("תאריך:", n));
        doc.add(dateRow);

        doc.close();
        writer.close();
    }

    private PdfPCell block(String title, String value, Font titleFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setPadding(10f);
        cell.setBorderWidth(1f);

        Paragraph p1 = new Paragraph(title, titleFont);
        p1.setAlignment(Element.ALIGN_RIGHT);

        Paragraph p2 = new Paragraph(safe(value), valueFont);
        p2.setAlignment(Element.ALIGN_RIGHT);

        cell.addElement(p1);
        cell.addElement(p2);
        return cell;
    }

    private PdfPCell signatureBox(String label, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setPadding(14f);
        cell.setBorderWidth(1f);

        Paragraph p = new Paragraph(label + " ____________________", font);
        p.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(p);

        return cell;
    }

    private void buildIncomingListPdf(OutputStream os,
                                      List<TransferRequest> requests,
                                      String toName,
                                      String toPn) throws Exception {

        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();

        Font title = hebrew(22, Font.BOLD);
        Font h = hebrew(12, Font.BOLD);
        Font n = hebrew(12, Font.NORMAL);
        Font small = hebrew(10, Font.NORMAL);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        PdfPCell titleCell = new PdfPCell(new Phrase("בקשות העברה נכנסות", title));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        titleCell.setPaddingBottom(10);

        titleTable.addCell(titleCell);

        doc.add(titleTable);



        PdfPTable who = new PdfPTable(1);
        who.setWidthPercentage(100);
        who.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        who.setSpacingBefore(8);
        who.setSpacingAfter(12);
        who.addCell(rtlCell("לכבוד: " + safe(toName) + " (" + safe(toPn) + ")", n, Element.ALIGN_RIGHT, false));
        doc.add(who);

        PdfPTable meta = new PdfPTable(1);
        meta.setWidthPercentage(100);
        meta.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        PdfPCell createdCell = rtlCell("נוצר בתאריך: " + fmt(LocalDateTime.now()), small, Element.ALIGN_LEFT, false);
        createdCell.setBorder(Rectangle.NO_BORDER);
        createdCell.setPadding(0);

        meta.addCell(createdCell);
        doc.add(meta);

        addSpacer(doc, 8);


        addSpacer(doc, 8);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.setSpacingBefore(8);
        table.setWidths(new float[]{14, 14, 18, 30, 24});

        table.addCell(rtlCell("בקשה", h, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("Item ID", h, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("סטטוס", h, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("הערה", h, Element.ALIGN_CENTER, true));
        table.addCell(rtlCell("נוצר", h, Element.ALIGN_CENTER, true));

        for (TransferRequest tr : requests) {
            table.addCell(rtlCell(String.valueOf(tr.getId()), n, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(String.valueOf(tr.getItemId()), n, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(String.valueOf(tr.getStatus()), n, Element.ALIGN_CENTER, false));
            table.addCell(rtlCell(safe(tr.getNote()), n, Element.ALIGN_RIGHT, false));
            table.addCell(rtlCell(fmt(tr.getCreatedAt()), n, Element.ALIGN_CENTER, false));
        }

        doc.add(table);

        doc.close();
        writer.close();
    }
}
