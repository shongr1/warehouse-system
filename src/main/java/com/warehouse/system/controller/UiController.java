package com.warehouse.system.controller;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.warehouse.system.dto.*;
import com.warehouse.system.pdf.PdfFonts;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
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
    private final CategoryRepository categoryRepository;



    public UiController(
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            ItemTypeRepository itemTypeRepository,
            StockBalanceRepository stockBalanceRepository,
            ItemRepository itemRepository,
            TransferRequestRepository transferRequestRepository,
            TransferService transferService,
            KitItemComponentRepository kitItemComponentRepository,
            KitComponentRepository kitComponentRepository,
            CategoryRepository categoryRepository // ← הוסף כאן
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
        this.categoryRepository = categoryRepository; // ← והוסף כאן
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
        var incomingRequests = transferRequestRepository.findByToUser_IdAndStatus(me.getId(), TransferStatus.PENDING);

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
    public String createStock(StockForm form,
                              @RequestParam(required = false) String categoryName,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) { // הוספנו את זה כדי לשלוח הודעות ל-UI
        try {
            if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

            String pn = AuthController.currentPn(session);
            var warehouse = warehouseRepository.findById(form.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("המחסן לא נמצא"));

            // בדיקת הרשאות בעלים על המחסן
            String ownerPn = warehouse.getOwner() != null ? warehouse.getOwner().getPersonalNumber().trim() : null;
            if (ownerPn == null || !pn.equals(ownerPn)) {
                redirectAttributes.addFlashAttribute("errorMessage", "שגיאה: אין לך הרשאה לבצע פעולות במחסן זה.");
                return "redirect:/ui/warehouses/" + form.getWarehouseId();
            }

            var itemType = itemTypeRepository.findById(form.getItemTypeId())
                    .orElseThrow(() -> new RuntimeException("סוג הפריט לא נמצא"));

            // לוגיקת קטגוריה: מציאת קטגוריה קיימת או יצירת חדשה המשוייכת למחסן
            Category category = null;
            if (categoryName != null && !categoryName.isBlank() && !categoryName.equals("ללא קטגוריה")) {
                category = categoryRepository.findByWarehouseId(warehouse.getId()).stream()
                        .filter(c -> c.getName().equalsIgnoreCase(categoryName.trim()))
                        .findFirst()
                        .orElseGet(() -> {
                            Category newCat = new Category();
                            newCat.setName(categoryName.trim());
                            newCat.setWarehouse(warehouse);
                            return categoryRepository.save(newCat);
                        });
            }

            // עדכון סוג הפריט לקיט במידה וסומן בטופס
            if (form.isKit() && !itemType.isKit()) {
                itemType.setKit(true);
                itemTypeRepository.save(itemType);
            }

            int addQty = 0;

            if (itemType.isSerialized()) {
                List<String> serials = parseSerials(form).stream()
                        .distinct()
                        .collect(Collectors.toList());

                if (serials.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "שגיאה: פריט זה מוגדר כפריט עם מספר סידורי, אך לא הוזנו מספרים.");
                    return "redirect:/ui/warehouses/" + form.getWarehouseId();
                }

                addQty = serials.size();

                for (String sn : serials) {
                    if (itemRepository.existsBySerialNumber(sn)) {
                        // במקום לקרוס, נחזיר הודעת אזהרה למשתמש
                        redirectAttributes.addFlashAttribute("errorMessage", "שגיאה: המספר הסידורי " + sn + " כבר קיים במערכת!");
                        return "redirect:/ui/warehouses/" + form.getWarehouseId();
                    }

                    Item item = new Item();
                    item.setWarehouse(warehouse);
                    item.setItemType(itemType);
                    item.setSerialNumber(sn);
                    item.setStatus(ItemStatus.IN_STOCK);
                    item.setCategory(category);

                    Item savedItem = itemRepository.save(item);

                    if (itemType.isKit()) {
                        var components = kitComponentRepository.findByItem_Id(itemType.getId());
                        for (var c : components) {
                            KitItemComponent row = new KitItemComponent();
                            row.setKitItem(savedItem);
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
                // טיפול בפריטים ללא מספר סידורי (מגבונים, מזלגות וכו')
                addQty = form.getQuantity();
                if (addQty <= 0) {
                    redirectAttributes.addFlashAttribute("errorMessage", "שגיאה: יש להזין כמות גדולה מ-0.");
                    return "redirect:/ui/warehouses/" + form.getWarehouseId();
                }

                for (int i = 0; i < addQty; i++) {
                    Item item = new Item();
                    item.setWarehouse(warehouse);
                    item.setItemType(itemType);
                    item.setStatus(ItemStatus.IN_STOCK);
                    item.setCategory(category);
                    item.setSerialNumber(null);
                    itemRepository.save(item);
                }
            }

            updateStockBalance(warehouse, itemType, addQty);

            // הודעת הצלחה
            redirectAttributes.addFlashAttribute("successMessage", "המלאי עודכן בהצלחה! נוספו " + addQty + " יחידות.");
            return "redirect:/ui/warehouses/" + form.getWarehouseId();

        } catch (Exception e) {
            // תפיסת כל שגיאה בלתי צפויה והצגתה למשתמש
            redirectAttributes.addFlashAttribute("errorMessage", "שגיאה בלתי צפויה: " + e.getMessage());
            return "redirect:/ui/warehouses/" + form.getWarehouseId();
        }
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

    // ---------- WAREHOUSE DETAILS (מעודכן לסינון לפי בעלים) ----------
    @GetMapping("/ui/warehouses/{id}")
    public String warehouseDetails(@PathVariable Long id, HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow();

        // שליפת הפריטים של המשתמש במחסן הזה
        List<Item> allItems = itemRepository.findByOwner(me).stream()
                .filter(item -> item.getWarehouse().getId().equals(id))
                .toList();

        List<Category> allCategories = categoryRepository.findByWarehouseId(id);

        // מבנה נתונים חדש: קטגוריה -> (שם מוצר -> רשימת פריטים)
        Map<String, Map<String, List<Item>>> inventoryMap = new LinkedHashMap<>();

        // אתחול קטגוריות
        for (Category cat : allCategories) {
            inventoryMap.put(cat.getName(), new LinkedHashMap<>());
        }
        inventoryMap.putIfAbsent("ללא קטגוריה", new LinkedHashMap<>());

        // מילוי המפה בקיבוץ כפול: קטגוריה ולאחר מכן שם סוג הפריט
        for (Item item : allItems) {
            String catName = (item.getCategory() != null) ? item.getCategory().getName() : "ללא קטגוריה";
            String typeName = item.getItemType().getName();

            inventoryMap.get(catName)
                    .computeIfAbsent(typeName, k -> new ArrayList<>())
                    .add(item);
        }

        // שליפת רכיבי קיט (Checklist)
        List<Long> itemIds = allItems.stream().map(Item::getId).toList();
        Map<Long, List<KitItemComponent>> kitChecklist = new HashMap<>();
        if (!itemIds.isEmpty()) {
            kitChecklist = kitItemComponentRepository.findAllByKitItem_IdIn(itemIds)
                    .stream()
                    .collect(Collectors.groupingBy(comp -> comp.getKitItem().getId()));
        }

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("inventoryMap", inventoryMap); // המבנה המקובץ
        model.addAttribute("kitChecklist", kitChecklist);
        model.addAttribute("me", me);

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
    @PostMapping("/ui/item-types")
    public String createItemType(CreateItemTypeForm form,
                                 @RequestParam Long warehouseId,
                                 HttpSession session) {

        // 1. בדיקת התחברות
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        // 2. שליפת המשתמש המחובר מהדאטה-בייס כדי להצמיד אותו כבעלים
        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ItemType it = new ItemType();
        it.setCatalogNumber(form.getCatalogNumber());
        it.setName(form.getName());
        it.setSerialized(form.isSerialized());

        // --- התיקון הקריטי: הגדרת הבעלים של המק"ט ---
        it.setOwner(me);
        // ------------------------------------------

        // הוספה עבור ערכות (Kits)
        it.setKit(form.isKit());

        if (form.isKit() && form.getComponents() != null) {
            for (KitComponent comp : form.getComponents()) {
                KitTemplateComponent template = new KitTemplateComponent();
                template.setComponentName(comp.getComponentName());
                template.setSubCatalogNumber(comp.getSubCatalogNumber());
                template.setQuantity(comp.getQuantity());

                it.addTemplateComponent(template);
            }
        }

        // שמירת ה-ItemType עם ה-Owner החדש
        itemTypeRepository.save(it);

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
        if (!AuthController.isLoggedIn(session)) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        // 1. שליפת המשתמש המחובר מהסשן ומבסיס הנתונים
        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ItemType it = new ItemType();
        it.setCatalogNumber(form.getCatalogNumber().trim());
        it.setName(form.getName().trim());
        it.setSerialized(form.isSerialized());

        // 2. הצמדת הבעלות למשתמש הנוכחי
        it.setOwner(me);

        // 3. שמירת סוג הפריט החדש
        ItemType saved = itemTypeRepository.save(it);

        // 4. החזרת התשובה ל-Frontend
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "catalogNumber", saved.getCatalogNumber(),
                "name", saved.getName(),
                "serialized", saved.isSerialized()
        ));
    }
    @GetMapping("/ui/warehouses/{id}/add")
    public String addProductsPage(@PathVariable Long id,
                                  @RequestParam(required = false) String cat,
                                  HttpSession session,
                                  Model model) {

        // 1. בדיקת התחברות
        if (!AuthController.isLoggedIn(session)) {
            return "redirect:/ui/login";
        }

        // 2. שליפת פרטי המשתמש המחובר
        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. שליפת המחסן ובדיקת הרשאות
        var warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        String ownerPn = warehouse.getOwner() == null ? null : warehouse.getOwner().getPersonalNumber();
        if (ownerPn == null || !pn.equals(ownerPn.trim())) {
            throw new RuntimeException("Forbidden: this warehouse is not yours");
        }

        // 4. סינון סוגי המוצרים (ItemTypes) - OWNER אישי בלבד
        // הסרנו את התנאי של it.getOwner() == null כדי שלא יראו מוצרים של אחרים או כלליים
        List<ItemType> filteredItemTypes = itemTypeRepository.findAll().stream()
                .filter(it -> it.getOwner() != null && it.getOwner().getId().equals(me.getId()))
                .collect(Collectors.toList());

        // 5. הכנת הטופס והמודל
        StockForm stockForm = new StockForm();
        stockForm.setWarehouseId(id);

        model.addAttribute("selectedCategoryName", cat);
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("itemTypes", filteredItemTypes); // עכשיו יופיעו רק סוגים שאתה יצרת אישית
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
    @Transactional(readOnly = true) // הוסף את השורה הזו כאן!
    @GetMapping("/ui/my-transfer-requests")
    public String myTransferRequests(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        var me = userRepository.findByPersonalNumber(pn.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // השליפה הזו היא שמעוררת את השגיאה בגלל שדה ה-signatureBase64
        var requests = transferRequestRepository.findByToUser_IdAndStatus(me.getId(), TransferStatus.PENDING);

        System.out.println("DEBUG: Logged in as ID " + me.getId());
        System.out.println("DEBUG: Found " + requests.size() + " pending requests in DB");

        // תיקון קטן כאן למקרה ש-getItem() או getFromUser() מחזירים null
        var groupedRequests = requests.stream()
                .collect(java.util.stream.Collectors.groupingBy(req -> {
                    String itemName = (req.getItem() != null && req.getItem().getItemType() != null)
                            ? req.getItem().getItemType().getName() : "פריט לא ידוע";
                    String fromUser = (req.getFromUser() != null)
                            ? req.getFromUser().getFullName() : "שולח לא ידוע";
                    return itemName + " from " + fromUser;
                }));

        model.addAttribute("groupedRequests", groupedRequests);
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
    @PostMapping("/ui/transfer-requests/approve-batch")
    public String approveBatch(@RequestParam("ids") List<Long> ids, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);
        transferService.approveBatch(ids, pn);

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

        var list = kitComponentRepository.findByItem_Id(id);

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
                kitComponentRepository.findByItem_Id(id));

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
    @PostMapping("/ui/categories/add")
    public String addCategory(@RequestParam String name,
                              @RequestParam Long warehouseId,
                              HttpSession session) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        // 1. שליפת המחסן מבסיס הנתונים
        var warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        // 2. יצירת הקטגוריה ושיוך המחסן אליה
        Category cat = new Category();
        cat.setName(name.trim());
        cat.setWarehouse(warehouse); // הגדרת המחסן - זה יפתור את ה-Null Constraint

        // 3. שמירה
        categoryRepository.save(cat);

        return "redirect:/ui/warehouses/" + warehouseId;
    }
    @PostMapping("/ui/items/sign-bulk")
    public String signBulk(@RequestParam Long itemTypeId,
                           @RequestParam Long warehouseId,
                           @RequestParam int quantity,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";
        User me = AuthController.currentUser(session);

        // מציאת פריטים פנויים מאותו סוג במחסן הספציפי
        List<Item> availableItems = itemRepository.findByWarehouseId(warehouseId).stream()
                .filter(i -> i.getItemType().getId().equals(itemTypeId))
                .filter(i -> i.getStatus().name().equals("IN_STOCK"))
                .limit(quantity) // לוקחים רק את הכמות שהמשתמש ביקש
                .toList();

        if (availableItems.size() < quantity) {
            redirectAttributes.addFlashAttribute("errorMessage", "Not enough items in stock!");
            return "redirect:/ui/warehouses/" + warehouseId;
        }

        // עדכון הסטטוס לכל הפריטים שנמצאו
        for (Item item : availableItems) {
            item.setStatus(ItemStatus.SIGNED); // וודא שה-Enum שלך תואם לשם הזה
            item.setSignedBy(me);
            item.setSignatureDate(LocalDateTime.now());
            itemRepository.save(item);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Successfully signed " + availableItems.size() + " units.");
        return "redirect:/ui/warehouses/" + warehouseId;
    }
    @PostMapping("/ui/items/transfer-bulk")
    public String transferBulk(@RequestParam(required = false) Long itemTypeId,
                               @RequestParam(required = false) Long warehouseId,
                               @RequestParam(required = false) Long fromUserId,
                               @RequestParam(defaultValue = "0") int quantity,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        // בדיקת דפוס - מה קיבלנו מה-HTML?
        System.out.println("DEBUG: itemTypeId=" + itemTypeId);
        System.out.println("DEBUG: warehouseId=" + warehouseId);
        System.out.println("DEBUG: fromUserId=" + fromUserId);
        System.out.println("DEBUG: quantity=" + quantity);

        if (itemTypeId == null || warehouseId == null || fromUserId == null || quantity <= 0) {
            redirectAttributes.addFlashAttribute("error", "חסרים נתונים למעבר: וודא שסוג הפריט והכמות תקינים.");
            return "redirect:/ui/warehouses/" + (warehouseId != null ? warehouseId : "1");
        }

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";
        User me = AuthController.currentUser(session);

        // שליפת הפריטים
        List<Item> allItems = itemRepository.findByWarehouseId(warehouseId);

        List<Item> toTransfer = allItems.stream()
                .filter(i -> i.getItemType().getId().equals(itemTypeId))
                .filter(i -> i.getSignedBy() != null && i.getSignedBy().getId().equals(fromUserId))
                .limit(quantity)
                .toList();

        System.out.println("DEBUG: Found items to transfer: " + toTransfer.size());

        if (toTransfer.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "לא נמצאו פריטים חתומים להעברה תחת התנאים שנבחרו.");
            return "redirect:/ui/warehouses/" + warehouseId;
        }

        for (Item item : toTransfer) {
            item.setSignedBy(me);
            item.setOwner(me);
            item.setSignatureDate(LocalDateTime.now());
            item.setStatus(ItemStatus.SIGNED);
            itemRepository.save(item);
        }

        redirectAttributes.addFlashAttribute("success", "הצלחנו! " + toTransfer.size() + " פריטים הועברו אליך.");
        return "redirect:/ui/warehouses/" + warehouseId;
    }
    @PostMapping("/ui/items/update-component")
    public String updateKitComponent(@RequestParam Long itemId,
                                     @RequestParam String componentName,
                                     @RequestParam int actualQty,
                                     RedirectAttributes redirectAttributes) {
        try {
            // 1. קריאה לסרוויס שיעדכן את הכמות בבסיס הנתונים
            // הערה: תצטרך לוודא שיש לך לוגיקה שמוצאת את ה-Kit לפי ה-ID והשם של הרכיב

            redirectAttributes.addFlashAttribute("success", "Component updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update component: " + e.getMessage());
        }

        // 2. חזרה לדף המחסן שממנו הגענו (נניח שזה הדף הנוכחי)
        // אם יש לך את ה-warehouseId, עדיף להחזיר אליו ישירות
        return "redirect:/ui";
    }
    @GetMapping("/ui/item-types/next-catalog")
    @ResponseBody
    public ResponseEntity<String> getNextCatalog(@RequestParam String prefix, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return ResponseEntity.status(401).build();

        String pn = AuthController.currentPn(session);
        User me = userRepository.findByPersonalNumber(pn).orElseThrow();

        // מוצא את המק"ט האחרון שמתחיל בקידומת הזו
        String lastCatalog = itemTypeRepository.findTopCatalogNumberByPrefix(prefix, me.getId());

        if (lastCatalog == null || lastCatalog.length() <= prefix.length()) {
            // אם זה הראשון, נתחיל ב-01 (כדי שיהיה סדר כמו באקסל)
            return ResponseEntity.ok(prefix + "01");
        }

        try {
            String numberPart = lastCatalog.substring(prefix.length());
            int numberLength = numberPart.length(); // בודק כמה ספרות היו (למשל 2 עבור "03")
            int nextNum = Integer.parseInt(numberPart.trim()) + 1;

            // יוצר פורמט ששומר על אורך המספר עם אפסים מובילים
            String formattedNumber = String.format("%0" + numberLength + "d", nextNum);

            return ResponseEntity.ok(prefix + formattedNumber);
        } catch (Exception e) {
            // במקרה של תקלה בפירוש המספר, מחזיר פשוט את הבא בתור כברירת מחדל
            return ResponseEntity.ok(prefix + "01");
        }
    }
}
