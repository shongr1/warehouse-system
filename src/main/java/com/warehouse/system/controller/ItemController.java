package com.warehouse.system.controller;

import com.warehouse.system.dto.StockForm;
import com.warehouse.system.service.StockService;
import com.warehouse.system.dto.CreateItemRequest;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping
    @ResponseBody
    public Item createItem(@RequestBody CreateItemRequest req, HttpSession session) {
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
        item.setOwner(currentUser);

        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        return itemRepository.save(item);
    }

    @GetMapping("/warehouse/{warehouseId}")
    @ResponseBody
    public List<Item> getWarehouseInventory(@PathVariable Long warehouseId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return List.of();
        return itemRepository.findByOwner(currentUser);
    }

    // מעודכן: הנתיב הסופי יהיה /items/ui/update-notes
    @PostMapping("/ui/update-notes")
    public String updateItemNotes(@RequestParam Long itemId,
                                  @RequestParam String status,
                                  @RequestParam String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            item.setStatus(ItemStatus.valueOf(status));
            item.setNotes(notes);
            itemRepository.save(item);

            redirectAttributes.addFlashAttribute("success", "הפריט עודכן בהצלחה!");
            return "redirect:/ui/warehouses/" + item.getWarehouse().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "שגיאה בעדכון הפריט: " + e.getMessage());
            return "redirect:/ui";
        }
    }

    // מעודכן: הנתיב הסופי יהיה /items/ui/update-component
    @PostMapping("/ui/update-component")
    public String updateComponent(@RequestParam Long itemId,
                                  @RequestParam String componentName,
                                  @RequestParam int actualQty,
                                  RedirectAttributes redirectAttributes) {
        try {
            stockService.updateKitComponent(itemId, componentName, actualQty);
            redirectAttributes.addFlashAttribute("success", "רכיב " + componentName + " עודכן בהצלחה");

            Item item = itemRepository.findById(itemId).orElse(null);
            if (item != null) return "redirect:/ui/warehouses/" + item.getWarehouse().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "שגיאה בעדכון: " + e.getMessage());
        }
        return "redirect:/ui";
    }

    @GetMapping("/ui/item-types/{id}/kit-components")
    @ResponseBody
    public List<KitTemplateComponent> getKitComponentsForUI(@PathVariable Long id) {
        return itemTypeRepository.findById(id)
                .map(ItemType::getKitTemplateComponents)
                .orElse(List.of());
    }

    @PostMapping("/ui/stocks")
    public String handleStockForm(@ModelAttribute("stockForm") StockForm form, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        try {
            stockService.processStockEntry(form, currentUser);
        } catch (Exception e) {
            return "redirect:/ui/add-products?error=" + e.getMessage();
        }

        return "redirect:/ui/warehouses/" + form.getWarehouseId();
    }
}