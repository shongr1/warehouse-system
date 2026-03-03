package com.warehouse.system.controller;

import com.warehouse.system.dto.CreateItemRequest;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;

    public ItemController(ItemRepository itemRepository,
                          ItemTypeRepository itemTypeRepository,
                          WarehouseRepository warehouseRepository,
                          CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping
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

        // שיוך לקטגוריה - חשוב כדי שהכפתורים לא ייעלמו
        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            item.setCategory(category);
        }

        return itemRepository.save(item);
    }

    // 1. שליפת פריטים לפי קטגוריה (בשביל הכפתורים)
    @GetMapping("/category/{categoryId}")
    public List<Item> getItemsByCategory(@PathVariable Long categoryId) {
        return itemRepository.findByCategoryId(categoryId);
    }

    // 2. פונקציה חדשה: שליפת כל המלאי של המחסן (כולל חתימות וכולל יתומים)
    @GetMapping("/warehouse/{warehouseId}")
    public List<Item> getWarehouseInventory(@PathVariable Long warehouseId) {
        return itemRepository.findByWarehouseId(warehouseId);
    }
}