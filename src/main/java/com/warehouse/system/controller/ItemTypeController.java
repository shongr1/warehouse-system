package com.warehouse.system.controller;

import com.warehouse.system.entity.ItemType;
import com.warehouse.system.repository.ItemTypeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/item-types")
public class ItemTypeController {

    private final ItemTypeRepository itemTypeRepository;

    public ItemTypeController(ItemTypeRepository itemTypeRepository) {
        this.itemTypeRepository = itemTypeRepository;
    }

    @PostMapping
    public ItemType create(@RequestBody ItemType itemType) {
        return itemTypeRepository.save(itemType);
    }

    @GetMapping
    public List<ItemType> getAll() {
        return itemTypeRepository.findAll();
    }
}
