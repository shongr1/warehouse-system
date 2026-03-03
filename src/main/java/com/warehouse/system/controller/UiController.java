package com.warehouse.system.controller;

import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.warehouse.system.dto.*;
import com.warehouse.system.pdf.PdfFonts;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import com.warehouse.system.repository.KitItemComponentRepository;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import com.warehouse.system.service.TransferService;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.warehouse.system.repository.TransferRequestRepository;

import java.util.List;
import java.util.Map;

@Controller
public class UiController {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ItemRepository itemRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final TransferService transferService;
    private final KitItemComponentRepository kitItemComponentRepository;
    private final KitComponentRepository kitComponentRepository;




    public UiController(
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            ItemTypeRepository itemTypeRepository,
            StockBalanceRepository stockBalanceRepository,
            ItemRepository itemRepository,
            TransferRequestRepository transferRequestRepository,
            TransferService transferService,
            KitItemComponentRepository kitItemComponentRepository,
            KitComponentRepository kitComponentRepository
    ) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.itemRepository = itemRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.transferService = transferService;

        this.kitItemComponentRepository = kitItemComponentRepository;
        this.kitComponentRepository = kitComponentRepository;
    }


    // ---------- HOME ----------
    @GetMapping("/ui")
    public String home(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        System.out.println("PN=" + pn);

        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var myWarehouses = warehouseRepository.findAllByOwner_PersonalNumber(pn);
        System.out.println("Warehouses count=" + myWarehouses.size());

        model.addAttribute("me", me);
        model.addAttribute("myWarehouses", myWarehouses);
        model.addAttribute("myStocks", stockBalanceRepository.findByWarehouseOwnerPersonalNumber(pn));
        model.addAttribute("isAdmin", AuthController.isAdmin(session));

        return "ui-home";
    }

    // ---------- MANAGERS (ADMIN ONLY) ----------
    @GetMapping("/ui/managers/new")
    public String newManager(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";
        if (!AuthController.isAdmin(session)) throw new RuntimeException("Forbidden");

        model.addAttribute("createManagerForm", new CreateManagerForm());
        return "manager-create";
    }

    @PostMapping("/ui/managers")
    public String createManager(CreateManagerForm form, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";
        if (!AuthController.isAdmin(session)) throw new RuntimeException("Forbidden");

        if (userRepository.findByPersonalNumber(form.getPersonalNumber().trim()).isPresent()) {
            throw new RuntimeException("Personal number already exists");
        }

        var encoder = new BCryptPasswordEncoder();

        User u = new User();
        u.setPersonalNumber(form.getPersonalNumber().trim());
        u.setFullName(form.getFullName().trim());
        u.setRole(UserRole.WAREHOUSE_MANAGER); // ✅ manager by default
        u.setPasswordHash(encoder.encode(form.getPassword()));

        userRepository.save(u);
        return "redirect:/ui";
    }

    @GetMapping("/ui/warehouses")
    public String warehouses(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        // שליפת המשתמש המחובר כדי להשתמש ב-ID שלו
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // שליפת המחסנים שבבעלותי (הקוד הקיים שלך)
        List<WarehouseDto> warehouses = warehouseRepository.findAllByOwner_PersonalNumber(pn)
                .stream()
                .map(w -> new WarehouseDto(
                        w.getId(),
                        w.getName(),
                        w.getLocation(),
                        w.getOwner() == null ? null :
                                new WarehouseDto.OwnerDto(
                                        w.getOwner().getId(),
                                        w.getOwner().getFullName(),
                                        w.getOwner().getPersonalNumber(),
                                        w.getOwner().getRole() == null ? null : w.getOwner().getRole().name()
                                )
                ))
                .toList();

        // --- השורה החדשה והקריטית ---
        // אנחנו שולפים את כל הבקשות שמוענות אליי (toUser) ובסטטוס ממתין
        var incomingRequests = transferRequestRepository.findByToUserIdAndStatus(me.getId(), TransferStatus.PENDING);

        model.addAttribute("warehouses", warehouses);
        model.addAttribute("incomingRequests", incomingRequests); // מוסיפים למודל עבור ה-HTML

        return "warehouses";
    }

    @GetMapping("/ui/warehouses/new")
    public String newWarehouse(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        model.addAttribute("createForm", new CreateWarehouseForm());
        return "warehouse-create";
    }

    @PostMapping("/ui/warehouses")
    public String createWarehouse(CreateWarehouseForm form, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var owner = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("Owner not found: " + pn));

        Warehouse warehouse = new Warehouse();
        warehouse.setName(form.getName().trim());
        warehouse.setLocation(form.getLocation().trim());
        warehouse.setOwner(owner);

        warehouseRepository.save(warehouse);
        return "redirect:/ui";
    }

    // ---------- MANAGER → WAREHOUSES + STOCK ----------
    @GetMapping("/ui/managers/warehouses")
    public String managerWarehouses(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found: " + pn));

        model.addAttribute("me", me);
        model.addAttribute("itemTypes", itemTypeRepository.findAll());
        model.addAttribute("stockForm", new StockForm());
        model.addAttribute("createItemTypeForm", new CreateItemTypeForm());
        model.addAttribute("warehouses", warehouseRepository.findAllByOwner_PersonalNumber(pn));
        model.addAttribute("stocks", stockBalanceRepository.findByWarehouseOwnerPersonalNumber(pn));

        return "manager-warehouses";
    }
    @Transactional
    @PostMapping("/ui/stocks")
    public String createStock(StockForm form, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        var warehouse = warehouseRepository.findById(form.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        String ownerPn = warehouse.getOwner() != null ? warehouse.getOwner().getPersonalNumber().trim() : null;
        if (ownerPn == null || !pn.equals(ownerPn)) {
            throw new RuntimeException("Forbidden: this warehouse is not yours");
        }

        var itemType = itemTypeRepository.findById(form.getItemTypeId())
                .orElseThrow(() -> new RuntimeException("ItemType not found"));

        if (form.isKit() && !itemType.isKit()) {
            itemType.setKit(true);
            itemTypeRepository.save(itemType);
        }

        int addQty = 0;

        if (itemType.isSerialized()) {
            List<String> serials = parseSerials(form).stream()
                    .distinct()
                    .collect(Collectors.toList());

            addQty = serials.size();

            for (String sn : serials) {
                if (itemRepository.existsBySerialNumber(sn)) {
                    throw new RuntimeException("Error: Serial number " + sn + " already exists!");
                }

                Item item = new Item();
                item.setWarehouse(warehouse);
                item.setItemType(itemType);
                item.setSerialNumber(sn);
                item.setStatus(ItemStatus.IN_STOCK);

                // שמירת הפריט הראשי
                Item savedItem = itemRepository.save(item);

                // יצירת רכיבי הקיט
                if (itemType.isKit()) {
                    var components = kitComponentRepository.findByKitType_Id(itemType.getId());
                    for (var c : components) {
                        KitItemComponent row = new KitItemComponent();
                        row.setKitItem(savedItem);

                        // ✅ התיקון הקריטי: חייבים לקשר לרכיב המקורי (ה-ID שחסר ב-DB)
                        row.setKitComponent(c);

                        row.setComponentName(c.getComponentName());
                        row.setSubCatalogNumber(c.getSubCatalogNumber());
                        row.setExpectedQty(c.getQuantity());
                        row.setActualQty(0);
                        row.setStatus(KitComponentStatus.MISSING);

                        kitItemComponentRepository.save(row);
                    }
                }
            }
        } else {
            addQty = form.getQuantity();
        }

        updateStockBalance(warehouse, itemType, addQty);
        return "redirect:/ui/warehouses/" + form.getWarehouseId();
    }

    // פונקציית עזר לעדכון המלאי הכללי - שומרת על ה-Controller נקי
    private void updateStockBalance(Warehouse warehouse, ItemType itemType, int addQty) {
        var sb = stockBalanceRepository.findByWarehouseIdAndItemTypeId(warehouse.getId(), itemType.getId())
                .orElseGet(() -> {
                    StockBalance newSb = new StockBalance();
                    newSb.setWarehouse(warehouse);
                    newSb.setItemType(itemType);
                    newSb.setQuantity(0);
                    return newSb;
                });

        sb.setQuantity(sb.getQuantity() + addQty);
        stockBalanceRepository.save(sb);
    }

    private List<String> parseSerials(StockForm form) {
        String mode = (form.getSerialMode() == null || form.getSerialMode().isBlank()) ? "AUTO" : form.getSerialMode();

        if ("MANUAL".equalsIgnoreCase(mode)) {
            if (form.getManualSerials() == null || form.getManualSerials().isBlank()) {
                throw new RuntimeException("Manual serials are required");
            }
            return java.util.Arrays.stream(form.getManualSerials().split("[,\\n\\r]+"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }

        if (form.getStartSerial() == null || form.getStartSerial().isBlank()) {
            throw new RuntimeException("Start serial is required");
        }
        if (form.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be >= 1");
        }

        long start;
        try {
            start = Long.parseLong(form.getStartSerial().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Start serial must be a number");
        }

        java.util.List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < form.getQuantity(); i++) out.add(String.valueOf(start + i));
        return out;
    }

    // ---------- WAREHOUSE DETAILS ----------
    @GetMapping("/ui/warehouses/{id}")
    public String warehouseDetails(@PathVariable Long id, HttpSession session, Model model) {
        // 1. בדיקה אם המשתמש מחובר בכלל
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        // 2. שליפת המשתמש המחובר מהסשן (באמצעות העזר שבנינו ב-AuthController)
        var me = AuthController.currentUser(session);

        // הגנה נוספת: אם משום מה ה-User אבד אבל ה-PN קיים (למקרה של סשן ישן)
        if (me == null) {
            String pn = AuthController.currentPn(session);
            if (pn != null) {
                me = userRepository.findByPersonalNumber(pn).orElse(null);
                session.setAttribute("user", me); // עדכון הסשן בזמן אמת
            }
        }

        // 3. הזרקת המשתמש ל-Model כדי שה-HTML יזהה את me
        model.addAttribute("me", me);

        // הדפסה ללוג לבדיקה (תוכל למחוק אחרי שזה עובד)
        System.out.println(">>> DEBUG: Current User (me): " + (me != null ? me.getFullName() : "NULL"));

        // 4. טעינת נתוני המחסן והפריטים
        var warehouse = warehouseRepository.findById(id).orElseThrow();
        var allItems = itemRepository.findByWarehouseId(id);
        var kitChecklist = new java.util.HashMap<Long, java.util.List<KitItemComponent>>();

        for (var it : allItems) {
            if (it.getItemType().isKit()) {
                var rows = kitItemComponentRepository.findByKitItem_IdOrderByKitComponent_Id(it.getId());
                kitChecklist.put(it.getId(), rows);
            }
        }

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("items", allItems);
        model.addAttribute("kitChecklist", kitChecklist);
        model.addAttribute("stocks", stockBalanceRepository.findByWarehouseId(id));

        return "warehouse-details";
    }
    /**  @GetMapping("/ui/warehouses/view")
    public String viewWarehouseStock(@RequestParam Long id, HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        String ownerPn = warehouse.getOwner() == null ? null : warehouse.getOwner().getPersonalNumber();
        ownerPn = ownerPn == null ? null : ownerPn.trim();
        if (ownerPn == null || !pn.equals(ownerPn)) throw new RuntimeException("Forbidden: this warehouse is not yours");

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("stocks", stockBalanceRepository.findByWarehouseId(id));
        return "warehouse-stock";
    }
**/
    // ---------- ITEM TYPES ----------
    @PostMapping("/ui/item-types")
    public String createItemType(CreateItemTypeForm form,
                                 @RequestParam Long warehouseId,
                                 HttpSession session) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        ItemType it = new ItemType();
        it.setCatalogNumber(form.getCatalogNumber());
        it.setName(form.getName());
        it.setSerialized(form.isSerialized());

        // --- הוספה עבור ערכות (Kits) ---
        it.setKit(form.isKit()); // וודא שב-Form יש שדה כזה

        if (form.isKit() && form.getComponents() != null) {
            for (KitComponent comp : form.getComponents()) {
                // שימוש במתודת ה-Helper שהגדרת ב-Entity (addComponent)
                // היא זו שקושרת את ה-comp ל-it (האבא)
                it.addComponent(comp);
            }
        }
        // -------------------------------

        itemTypeRepository.save(it); // בזכות CascadeType.ALL, זה ישמור גם את הרכיבים

        return "redirect:/ui/warehouses/" + warehouseId + "/add";
    }


    @PostMapping("/ui/stocks/delete")
    public String deleteStock(@RequestParam Long stockBalanceId, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var sb = stockBalanceRepository.findById(stockBalanceId)
                .orElseThrow(() -> new RuntimeException("StockBalance not found"));

        String ownerPn = sb.getWarehouse() == null || sb.getWarehouse().getOwner() == null
                ? null
                : sb.getWarehouse().getOwner().getPersonalNumber();

        ownerPn = ownerPn == null ? null : ownerPn.trim();

        if (ownerPn == null || !pn.equals(ownerPn)) throw new RuntimeException("Forbidden: not your stock");

        stockBalanceRepository.deleteById(stockBalanceId);
        return "redirect:/ui/managers/warehouses";
    }

    @PostMapping("/ui/item-types/ajax")
    public ResponseEntity<?> createItemTypeAjax(CreateItemTypeForm form, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return ResponseEntity.status(401).body("Not logged in");

        ItemType it = new ItemType();
        it.setCatalogNumber(form.getCatalogNumber().trim());
        it.setName(form.getName().trim());
        it.setSerialized(form.isSerialized());

        ItemType saved = itemTypeRepository.save(it);

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "catalogNumber", saved.getCatalogNumber(),
                "name", saved.getName(),
                "serialized", saved.isSerialized()
        ));
    }
    @GetMapping("/ui/warehouses/{id}/add")
    public String addProductsPage(@PathVariable Long id,
                                  HttpSession session,
                                  Model model) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        String ownerPn = warehouse.getOwner() == null ? null : warehouse.getOwner().getPersonalNumber();
        ownerPn = ownerPn == null ? null : ownerPn.trim();

        if (ownerPn == null || !pn.equals(ownerPn)) {
            throw new RuntimeException("Forbidden: this warehouse is not yours");
        }

        StockForm stockForm = new StockForm();
        stockForm.setWarehouseId(id);

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("itemTypes", itemTypeRepository.findAll());
        model.addAttribute("stockForm", stockForm);
        model.addAttribute("createItemTypeForm", new CreateItemTypeForm());

        return "warehouse-add-products";
    }



    @GetMapping("/ui/warehouses/view")
    public String legacyView(@RequestParam Long id) {
        return "redirect:/ui/warehouses/" + id;
    }

    @GetMapping("/ui/admin")
    public String adminDashboard(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";
        if (!AuthController.isAdmin(session)) throw new RuntimeException("Forbidden");

        // כל המשתמשים (אפשר לסנן רק Managers)
        var managers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.WAREHOUSE_MANAGER)
                .toList();

        // כל המחסנים
        var warehouses = warehouseRepository.findAll();

        // נתונים מהירים
        model.addAttribute("managers", managers);
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("managersCount", managers.size());
        model.addAttribute("warehousesCount", warehouses.size());

        return "admin-dashboard";
    }
    @PostMapping("/ui/items/{id}/sign")
    public String signItemToMe(@PathVariable Long id,
                               @RequestHeader(value = "Referer", required = false) String referer,
                               HttpSession session) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        transferService.signToMe(id, pn);

        return "redirect:" + (referer != null ? referer : "/ui");
    }

    @GetMapping("/ui/my-transfer-requests")
    public String myTransferRequests(HttpSession session, Model model) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var requests = transferRequestRepository.findByToUserIdAndStatus(me.getId(), TransferStatus.PENDING);

        model.addAttribute("requests", requests);

        return "my-transfer-requests";
    }
    @PostMapping("/ui/transfer-requests/{id}/approve")
    public String approveTransfer(@PathVariable Long id, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        transferService.approve(id, pn);

        return "redirect:/ui/my-transfer-requests";
    }

    @PostMapping("/ui/transfer-requests/{id}/reject")
    public String rejectTransfer(@PathVariable Long id, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        transferService.reject(id, pn);

        return "redirect:/ui/my-transfer-requests";
    }
    @GetMapping("/ui/my-signed-items")
    public String mySignedItems(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // הפיכת ה-Entities ל-DTOs עם הלוגיקה של חישוב הימים
        List<ItemDto> dtos = itemRepository.findBySignedBy_Id(me.getId())
                .stream()
                .map(ItemDto::fromEntity) // וודא שהגדרת את המתודה הזו ב-ItemDto
                .toList();

        model.addAttribute("items", dtos);
        return "my-signed-items";
    }
    /**
    @GetMapping("/ui/my-transfer-requests/pdf")
    public ResponseEntity<byte[]> exportIncomingTransfersPdf(HttpSession session) {
        if (!AuthController.isLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }
        String pn = AuthController.currentPn(session);

        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found: " + pn));

        var requests = transferRequestRepository
                .findAllByToUserIdAndStatusOrderByCreatedAtDesc(me.getId(), TransferStatus.PENDING);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 26, 26);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            BaseFont bf = PdfFonts.hebrewBaseFont();
            Font titleFont = new Font(bf, 18, Font.BOLD);
            Font normal = new Font(bf, 11, Font.NORMAL);
            Font bold = new Font(bf, 11, Font.BOLD);

            // כותרת
            Paragraph title = new Paragraph("טופס העברת חתימה - בקשות נכנסות", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph(" ", normal));

            Paragraph meta = new Paragraph(
                    "מקבל: " + me.getFullName() + "  |  מספר אישי: " + me.getPersonalNumber(),
                    normal
            );
            meta.setAlignment(Element.ALIGN_CENTER);
            doc.add(meta);

            doc.add(new Paragraph(" ", normal));

            // טבלה
            PdfPTable table = new PdfPTable(new float[]{1.2f, 2.2f, 3.2f, 2.2f, 3.5f, 2.6f});
            table.setWidthPercentage(100);
            table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            addHeader(table, "מס׳ בקשה", bold);
            addHeader(table, "סטטוס", bold);
            addHeader(table, "פריט", bold);
            addHeader(table, "סיריאל", bold);
            addHeader(table, "הערה", bold);
            addHeader(table, "תאריך", bold);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (var tr : requests) {
                // מביאים פריט כדי להוציא שם+סיריאל
                var item = itemRepository.findById(tr.getItemId()).orElse(null);

                String itemName = (item == null || item.getItemType() == null) ? "-" : item.getItemType().getName();
                String serial = (item == null) ? "-" : item.getSerialNumber();

                addCell(table, String.valueOf(tr.getId()), normal);
                addCell(table, String.valueOf(tr.getStatus()), normal);
                addCell(table, itemName, normal);
                addCell(table, serial, normal);
                addCell(table, tr.getNote() == null ? "" : tr.getNote(), normal);
                addCell(table, tr.getCreatedAt() == null ? "" : tr.getCreatedAt().format(fmt), normal);
            }

            doc.add(table);

            doc.add(new Paragraph(" ", normal));
            doc.add(new Paragraph("חתימת מקבל: ____________________    תאריך: ________________", normal));
            doc.add(new Paragraph("חתימת מוסר: ____________________    תאריך: ________________", normal));

            doc.close();
            writer.close();

            byte[] pdfBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=incoming-transfer-requests.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            throw new RuntimeException("PDF export failed", e);
        }
    }
**/
    @GetMapping("/ui/item-types/{id}/kit-components")
    @ResponseBody
    public ResponseEntity<?> kitComponents(@PathVariable Long id, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        var list = kitComponentRepository.findByKitType_Id(id);

        // הכי נקי: DTO ולא Map (מונע גם את השגיאת generics שהייתה לך)
        var out = list.stream().map(c -> new KitComponentDto(
                c.getId(),
                c.getComponentName(),
                c.getSubCatalogNumber(),
                c.getQuantity()
        )).toList();

        return ResponseEntity.ok(out);
    }

    public record KitComponentDto(Long id, String name, String subCatalog, int expectedQty) {}
    private static void addHeader(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(8);
        c.setBackgroundColor(new Color(30, 35, 55));
        c.setBorderColor(new Color(255,255,255,25));
        t.addCell(c);
    }

    private static void addCell(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(8);
        c.setBorderColor(new Color(255,255,255,25));
        t.addCell(c);
    }
    @GetMapping("/ui/item-types/{id}/kit-components/manage")
    public String manageKitComponents(@PathVariable Long id,
                                      HttpSession session,
                                      Model model) {

        if (!AuthController.isLoggedIn(session))
            return "redirect:/ui/login";

        var itemType = itemTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItemType not found"));

        if (!itemType.isKit())
            throw new RuntimeException("Not a KIT");

        model.addAttribute("kitType", itemType);
        model.addAttribute("components",
                kitComponentRepository.findByKitType_Id(id));

        return "kit-components-manage";
    }
    @PostMapping("/ui/item-types/{id}/kit-components")
    public String addKitComponent(@PathVariable Long id,
                                  @RequestParam String componentName,
                                  @RequestParam String subCatalogNumber,
                                  @RequestParam int quantity,
                                  HttpSession session) {

        if (!AuthController.isLoggedIn(session))
            return "redirect:/ui/login";

        var kitType = itemTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItemType not found"));

        KitComponent kc = new KitComponent();
        kc.setKitType(kitType);
        kc.setComponentName(componentName.trim());
        kc.setSubCatalogNumber(subCatalogNumber.trim());
        kc.setQuantity(quantity);

        kitComponentRepository.save(kc);

        return "redirect:/ui/item-types/" + id + "/kit-components/manage";
    }
    @PostMapping("/ui/kit-components/{id}/delete")
    public String deleteKitComponent(@PathVariable Long id,
                                     HttpSession session) {

        if (!AuthController.isLoggedIn(session))
            return "redirect:/ui/login";

        var kc = kitComponentRepository.findById(id)
                .orElseThrow();

        Long kitId = kc.getKitType().getId();

        kitComponentRepository.delete(kc);

        return "redirect:/ui/item-types/" + kitId + "/kit-components/manage";
    }
    @PostMapping("/ui/kit-checklist/{rowId}/update")
    public String updateKitChecklistRow(@PathVariable Long rowId,
                                        @RequestParam int actualQty,
                                        @RequestHeader(value = "Referer", required = false) String referer) {

        var row = kitItemComponentRepository.findById(rowId)
                .orElseThrow(() -> new RuntimeException("Checklist row not found"));

        row.setActualQty(actualQty);

        // התאמה ל-Enum שלך: OK, MISSING, DAMAGED
        if (actualQty >= row.getExpectedQty()) {
            row.setStatus(KitComponentStatus.OK);
        } else {
            row.setStatus(KitComponentStatus.MISSING);
        }

        kitItemComponentRepository.save(row);

        return "redirect:" + (referer != null ? referer : "/ui");

    }
    @PostMapping("/ui/items/{id}/unsign")
    public String unsignItem(@PathVariable Long id, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        var item = itemRepository.findById(id).orElseThrow();

        // החזרת הסטטוס למלאי ומחיקת המשתמש החתום
        item.setStatus(com.warehouse.system.entity.ItemStatus.IN_STOCK);
        item.setSignedBy(null);
        itemRepository.save(item);

        // חזרה למחסן שבו היינו
        return "redirect:/ui/warehouses/" + item.getWarehouse().getId();
    }
}
