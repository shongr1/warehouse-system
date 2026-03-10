package com.warehouse.system.controller;

import com.warehouse.system.service.StockService;
import com.warehouse.system.dto.CreateItemRequest;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import org.springframework.stereotype.Controller; // שינוי ל-@Controller כדי לתמוך ב-Redirect של ה-UI
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller // משתמשים ב-Controller רגיל כדי שיוכל להחזיר RedirectView ודפי HTML
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;
    private final StockService stockService; // הזרקה של ה-Service שחזרה קודם

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

    // API ליצירת פריט (אם אתה קורא לזה מ-Postman/JS)
    @PostMapping
    @ResponseBody // מחזיר JSON למרות שהקלאס הוא @Controller
    public Item createItem(@RequestBody CreateItemRequest req) {
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

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        return itemRepository.save(item);
    }

    @GetMapping("/category/{categoryId}")
    @ResponseBody
    public List<Item> getItemsByCategory(@PathVariable Long categoryId) {
        return itemRepository.findByCategoryId(categoryId);
    }

    @GetMapping("/warehouse/{warehouseId}")
    @ResponseBody
    public List<Item> getWarehouseInventory(@PathVariable Long warehouseId) {
        return itemRepository.findByWarehouseId(warehouseId);
    }

    @PostMapping("/update-notes")
    public RedirectView updateItemNotes(@RequestParam Long itemId,
                                        @RequestParam String notes,
                                        @RequestParam String status) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        item.setNotes(notes);

        try {
            item.setStatus(ItemStatus.valueOf(status));
        } catch (Exception e) {
            // התעלמות משגיאת המרה
        }

        itemRepository.save(item);
        return new RedirectView("/ui/warehouses/" + item.getWarehouse().getId());
    }

    // התיקון הקריטי לעדכון הרכיבים בערכה
    @PostMapping("/ui/items/update-component")
    public String updateComponent(@RequestParam Long itemId,
                                  @RequestParam String componentName,
                                  @RequestParam int actualQty,
                                  RedirectAttributes redirectAttributes) {
        try {
            // עכשיו stockService מוזרק ותקין
            stockService.updateKitComponent(itemId, componentName, actualQty);
            redirectAttributes.addFlashAttribute("success", "רכיב " + componentName + " עודכן בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "שגיאה בעדכון: " + e.getMessage());
        }

        // מחזיר אותך חזרה לדף הראשי של ה-UI
        return "redirect:/ui";
    }
}