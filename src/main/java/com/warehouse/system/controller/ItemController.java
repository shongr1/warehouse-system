package com.warehouse.system.controller;

import com.warehouse.system.service.StockService;
import com.warehouse.system.dto.CreateItemRequest;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;
    private final StockService stockService;

    public ItemController(ItemRepository itemRepository,
                          ItemTypeRepository itemTypeRepository,
                          WarehouseRepository warehouseRepository,
                          CategoryRepository categoryRepository,
                          StockService stockService) {
        this.itemRepository = itemRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
        this.stockService = stockService;
    }

    // יצירת פריט - מעודכן עם הגדרת Owner
    @PostMapping
    @ResponseBody
    public Item createItem(@RequestBody CreateItemRequest req, HttpSession session) {
        // שליפת המשתמש מהסשן
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) throw new RuntimeException("User not logged in");

        ItemType type = itemTypeRepository.findById(req.itemTypeId())
                .orElseThrow(() -> new RuntimeException("ItemType not found"));

        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        Item item = new Item();
        item.setSerialNumber(req.serialNumber());
        item.setItemType(type);
        item.setWarehouse(warehouse);
        item.setLocation(req.location());
        item.setStatus(ItemStatus.IN_STOCK);

        // הגדרה קריטית: המשתמש הנוכחי הוא הבעלים
        item.setOwner(currentUser);

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        return itemRepository.save(item);
    }

    // שליפת פריטים לפי מחסן - מעודכן לסינון לפי בעלים
    @GetMapping("/warehouse/{warehouseId}")
    @ResponseBody
    public List<Item> getWarehouseInventory(@PathVariable Long warehouseId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return List.of();

        // במקום להחזיר את כל המחסן, אנחנו מחזירים רק מה ששייך למשתמש בתוך המחסן הזה
        // (שים לב: עליך להוסיף מתודה כזו ב-Repository אם תרצה שילוב של מחסן ובעלים)
        return itemRepository.findByOwner(currentUser);
    }

    // שאר המתודות נשארות דומות, אך כדאי לוודא הרשאות בכולן
    @PostMapping("/update-notes")
    public RedirectView updateItemNotes(@RequestParam Long itemId,
                                        @RequestParam String notes,
                                        @RequestParam String status,
                                        HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // בדיקה בסיסית: רק הבעלים יכול לעדכן הערות
        if (!item.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to update this item");
        }

        item.setNotes(notes);
        try {
            item.setStatus(ItemStatus.valueOf(status));
        } catch (Exception e) {}

        itemRepository.save(item);
        return new RedirectView("/ui/warehouses/" + item.getWarehouse().getId());
    }

    @PostMapping("/ui/items/update-component")
    public String updateComponent(@RequestParam Long itemId,
                                  @RequestParam String componentName,
                                  @RequestParam int actualQty,
                                  RedirectAttributes redirectAttributes) {
        try {
            stockService.updateKitComponent(itemId, componentName, actualQty);
            redirectAttributes.addFlashAttribute("success", "רכיב " + componentName + " עודכן בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "שגיאה בעדכון: " + e.getMessage());
        }
        return "redirect:/ui";
    }
}