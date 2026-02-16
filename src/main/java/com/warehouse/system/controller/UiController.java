package com.warehouse.system.controller;

import com.warehouse.system.dto.CreateItemTypeForm;
import com.warehouse.system.dto.CreateManagerForm;
import com.warehouse.system.dto.CreateWarehouseForm;
import com.warehouse.system.dto.StockForm;
import com.warehouse.system.dto.WarehouseDto;
import com.warehouse.system.entity.ItemType;
import com.warehouse.system.entity.StockBalance;
import com.warehouse.system.entity.User;
import com.warehouse.system.entity.UserRole;
import com.warehouse.system.entity.Warehouse;
import com.warehouse.system.repository.*;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class UiController {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ItemRepository itemRepository;

    public UiController(
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            ItemTypeRepository itemTypeRepository,
            StockBalanceRepository stockBalanceRepository,
            ItemRepository itemRepository
    ) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.itemRepository = itemRepository;
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

    // ---------- WAREHOUSES ----------
    @GetMapping("/ui/warehouses")
    public String warehouses(HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

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

        model.addAttribute("warehouses", warehouses);
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

    // ---------- STOCK ----------
    @PostMapping("/ui/stocks")
    public String createStock(StockForm form, HttpSession session) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var warehouse = warehouseRepository.findById(form.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        // ✅ SECURITY: compare trimmed values
        String ownerPn = warehouse.getOwner() == null ? null : warehouse.getOwner().getPersonalNumber();
        ownerPn = ownerPn == null ? null : ownerPn.trim();
        if (ownerPn == null || !pn.equals(ownerPn)) {
            throw new RuntimeException("Forbidden: this warehouse is not yours");
        }

        var itemType = itemTypeRepository.findById(form.getItemTypeId())
                .orElseThrow(() -> new RuntimeException("ItemType not found"));

        int addQty;

        if (itemType.isSerialized()) {
            var serials = parseSerials(form);
            addQty = serials.size();

            for (String sn : serials) {
                if (itemRepository.existsByItemType_IdAndSerialNumber(itemType.getId(), sn)) {
                    throw new RuntimeException("Serial already exists: " + sn);
                }

                var item = new com.warehouse.system.entity.Item();
                item.setWarehouse(warehouse);
                item.setItemType(itemType);
                item.setSerialNumber(sn);

                itemRepository.save(item);
            }
        } else {
            addQty = form.getQuantity();
        }

        var existing = stockBalanceRepository.findByWarehouseIdAndItemTypeId(warehouse.getId(), itemType.getId());

        if (existing.isPresent()) {
            var sb = existing.get();
            sb.setQuantity(sb.getQuantity() + addQty);
            stockBalanceRepository.save(sb);
        } else {
            var sb = new StockBalance();
            sb.setWarehouse(warehouse);
            sb.setItemType(itemType);
            sb.setQuantity(addQty);
            stockBalanceRepository.save(sb);
        }

        // ✅ Redirect back to warehouse details (NOT /add)
        return "redirect:/ui/warehouses/" + form.getWarehouseId();
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

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String pn = AuthController.currentPn(session);

        var warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        String ownerPn = warehouse.getOwner() == null ? null : warehouse.getOwner().getPersonalNumber();
        ownerPn = ownerPn == null ? null : ownerPn.trim();

        if (ownerPn == null || !pn.equals(ownerPn)) {
            throw new RuntimeException("Forbidden: this warehouse is not yours");
        }

        model.addAttribute("warehouse", warehouse);
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


}
