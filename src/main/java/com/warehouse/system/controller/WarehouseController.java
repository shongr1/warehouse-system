package com.warehouse.system.controller;

import com.warehouse.system.dto.UpdateWarehouseRequest;
import com.warehouse.system.dto.WarehouseDto;
import com.warehouse.system.entity.User;
import com.warehouse.system.entity.Warehouse;
import com.warehouse.system.repository.UserRepository;
import com.warehouse.system.repository.WarehouseRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public WarehouseController(WarehouseRepository warehouseRepository,
                               UserRepository userRepository) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    // יצירת מחסן עם בעלים (User)
    @PostMapping
    public WarehouseDto create(@RequestBody CreateWarehouseRequest req) {

        User owner = userRepository.findByPersonalNumber(req.ownerPersonalNumber())
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + req.ownerPersonalNumber())
                );

        Warehouse warehouse = new Warehouse();
        warehouse.setName(req.name());
        warehouse.setLocation(req.location());
        warehouse.setOwner(owner);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toDto(saved);
    }

    // כל המחסנים (לניהול / אדמין)
    @GetMapping
    public List<WarehouseDto> getAll() {
        return warehouseRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }
    // המחסן שלי לפי מספר אישי
    @GetMapping("/my")
    public List<WarehouseDto> getMyWarehouses(@RequestParam String personalNumber) {

        List<Warehouse> warehouses =
                warehouseRepository.findAllByOwner_PersonalNumber(personalNumber.trim());

        if (warehouses.isEmpty()) {
            throw new RuntimeException("No warehouses found for user: " + personalNumber);
        }

        return warehouses.stream()
                .map(this::toDto)
                .toList();
    }


    private WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(
                w.getId(),
                w.getName(),
                w.getLocation(),
                toOwnerDto(w.getOwner())
        );
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
