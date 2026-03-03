package com.warehouse.system.controller;

import com.warehouse.system.dto.UpdateWarehouseRequest;
import com.warehouse.system.dto.WarehouseDto;
import com.warehouse.system.entity.Category;
import com.warehouse.system.entity.Item;
import com.warehouse.system.entity.User;
import com.warehouse.system.entity.Warehouse;
import com.warehouse.system.repository.CategoryRepository;
import com.warehouse.system.repository.ItemRepository;
import com.warehouse.system.repository.UserRepository;
import com.warehouse.system.repository.WarehouseRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository; // הוספנו את הריפו של הפריטים

    // Constructor מעודכן עם כל ה-Repositories
    public WarehouseController(WarehouseRepository warehouseRepository,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ItemRepository itemRepository) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }
    // --- קטגוריות ---

    // יצירת קטגוריה חדשה בתוך מחסן
    @PostMapping("/{id}/categories")
    public Category createCategory(@PathVariable Long id, @RequestParam String name) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + id));

        Category category = new Category();
        category.setName(name);
        category.setWarehouse(warehouse);

        return categoryRepository.save(category);
    }
    @GetMapping("/{id}/inventory-by-category")
    public Map<String, List<Item>> getInventoryByCategory(@PathVariable Long id) {
        // 1. מוודאים שהמחסן קיים
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        // 2. שולפים את כל הפריטים של המחסן הזה
        List<Item> allItems = itemRepository.findByWarehouseId(id);

        // 3. מארגנים את הפריטים לפי שם הקטגוריה
        // פריט ללא קטגוריה ייכנס תחת המפתח "כללי"
        return allItems.stream().collect(Collectors.groupingBy(item ->
                item.getCategory() != null ? item.getCategory().getName() : "ללא קטגוריה"
        ));
    }
    // שליפת כל הקטגוריות של מחסן מסוים
    @GetMapping("/{id}/categories")
    public List<Category> getCategoriesByWarehouse(@PathVariable Long id) {
        return categoryRepository.findByWarehouseId(id);
    }

    // --- מחסנים (מעודכן) ---

    @PostMapping
    public WarehouseDto create(@RequestBody CreateWarehouseRequest req) {
        User owner = userRepository.findByPersonalNumber(req.ownerPersonalNumber())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.ownerPersonalNumber()));

        Warehouse warehouse = new Warehouse();
        warehouse.setName(req.name());
        warehouse.setLocation(req.location());
        warehouse.setOwner(owner);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toDto(saved);
    }

    @GetMapping
    public List<WarehouseDto> getAll() {
        return warehouseRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/my")
    public List<WarehouseDto> getMyWarehouses(@RequestParam String personalNumber) {
        List<Warehouse> warehouses = warehouseRepository.findAllByOwner_PersonalNumber(personalNumber.trim());

        if (warehouses.isEmpty()) {
            throw new RuntimeException("No warehouses found for user: " + personalNumber);
        }

        return warehouses.stream()
                .map(this::toDto)
                .toList();
    }

    @PutMapping("/{id}")
    public WarehouseDto update(@PathVariable Long id, @RequestBody UpdateWarehouseRequest req) {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + id));

        if (req.name() != null && !req.name().isBlank()) {
            w.setName(req.name());
        }
        if (req.location() != null && !req.location().isBlank()) {
            w.setLocation(req.location());
        }

        Warehouse saved = warehouseRepository.save(w);
        return toDto(saved);
    }

    // --- Helper Methods ---

    private WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(
                w.getId(),
                w.getName(),
                w.getLocation(),
                toOwnerDto(w.getOwner())
        );
    }

    private WarehouseDto.OwnerDto toOwnerDto(User u) {
        if (u == null) return null;
        return new WarehouseDto.OwnerDto(
                u.getId(),
                u.getFullName(),
                u.getPersonalNumber(),
                u.getRole().name()
        );
    }
}