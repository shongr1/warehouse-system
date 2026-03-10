package com.warehouse.system.service;

import com.warehouse.system.dto.StockForm;
import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private StockBalanceRepository stockBalanceRepository;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Transactional
    public void addStock(StockForm form) {
        Warehouse warehouse = warehouseRepository.findById(form.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        ItemType itemType = itemTypeRepository.findById(form.getItemTypeId())
                .orElseThrow(() -> new RuntimeException("Item Type not found"));

        List<String> manualSerials = parseSerials(form);
        int quantityToAdd = (manualSerials.isEmpty()) ? form.getQuantity() : manualSerials.size();

        if (manualSerials.isEmpty() && (form.getPrefix() == null || form.getPrefix().isBlank())) {
            form.setPrefix(itemType.getName().substring(0, Math.min(itemType.getName().length(), 3)).toUpperCase() + "-");
        }

        Integer lastNumber = 0;
        if (manualSerials.isEmpty()) {
            lastNumber = itemRepository.findMaxNumberByPrefix(form.getPrefix() + "%");
            if (lastNumber == null) lastNumber = 0;
        }

        for (int i = 0; i < quantityToAdd; i++) {
            Item item = new Item();
            item.setItemType(itemType);
            item.setWarehouse(warehouse);
            item.setStatus(ItemStatus.IN_STOCK);

            if (!manualSerials.isEmpty()) {
                String sn = manualSerials.get(i);
                if (itemRepository.existsBySerialNumber(sn)) {
                    throw new RuntimeException("סריאל כפול! המספר " + sn + " כבר קיים.");
                }
                item.setSerialNumber(sn);
            } else {
                String internalId = form.getPrefix() + String.format("%02d", lastNumber + i + 1);
                item.setInternalCatalogId(internalId);
            }

            // לוגיקה חדשה: אם זה Kit, ניצור לו את הרכיבים ההתחלתיים מהתקן של ה-ItemType
            if (itemType.isKit() && itemType.getKitTemplateComponents() != null) {
                for (KitTemplateComponent template : itemType.getKitTemplateComponents()) {
                    KitComponent comp = new KitComponent();
                    comp.setItem(item);
                    comp.setComponentName(template.getComponentName());
                    comp.setSubCatalogNumber(template.getSubCatalogNumber());
                    comp.setExpectedQuantity(template.getQuantity());
                    comp.setActualQuantity(template.getQuantity()); // בהתחלה הערכה מלאה
                    comp.setStatus("AVAILABLE");
                    item.getComponents().add(comp);
                }
            }

            itemRepository.save(item);
        }

        updateBalance(warehouse, itemType, quantityToAdd);
    }

    @Transactional
    public void updateKitComponent(Long itemId, String componentName, int actualQty) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // חיפוש הרכיב בתוך ה-Item
        KitComponent targetComp = item.getComponents().stream()
                .filter(c -> c.getComponentName().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Component " + componentName + " not found in kit"));

        targetComp.setActualQuantity(actualQty);

        // עדכון סטטוס אוטומטי לפי הכמות
        if (actualQty < targetComp.getExpectedQuantity()) {
            targetComp.setStatus("MISSING");
        } else if (actualQty > targetComp.getExpectedQuantity()) {
            targetComp.setStatus("OVER_STOCK"); // אופציונלי
        } else {
            targetComp.setStatus("AVAILABLE");
        }

        itemRepository.save(item);
    }

    private void updateBalance(Warehouse warehouse, ItemType itemType, int addedQty) {
        StockBalance balance = stockBalanceRepository
                .findByWarehouseIdAndItemTypeId(warehouse.getId(), itemType.getId())
                .orElse(new StockBalance());

        if (balance.getId() == null) {
            balance.setWarehouse(warehouse);
            balance.setItemType(itemType);
            balance.setQuantity(0);
        }

        balance.setQuantity(balance.getQuantity() + addedQty);
        stockBalanceRepository.save(balance);
    }

    private List<String> parseSerials(StockForm form) {
        if (form.getManualSerials() == null || form.getManualSerials().isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(form.getManualSerials().split("[,\n\r]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}