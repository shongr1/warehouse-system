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

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional
    public void processStockEntry(StockForm form, User currentUser) {
        Warehouse warehouse = warehouseRepository.findById(form.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        ItemType itemType = itemTypeRepository.findById(form.getItemTypeId())
                .orElseThrow(() -> new RuntimeException("Item Type not found"));

        Category category = null;
        if (form.getCategoryName() != null && !form.getCategoryName().isEmpty()) {
            category = categoryRepository.findByName(form.getCategoryName()).orElse(null);
        }

        List<String> manualSerials = parseSerials(form);
        int quantityToAdd = (manualSerials.isEmpty()) ? form.getQuantity() : manualSerials.size();

        // לוגיקה ליצירת הפריטים
        for (int i = 0; i < quantityToAdd; i++) {
            Item item = new Item();
            item.setItemType(itemType);
            item.setWarehouse(warehouse);
            item.setCategory(category);
            item.setOwner(currentUser);
            item.setStatus(ItemStatus.IN_STOCK);

            // טיפול במספרים סידוריים
            if (!manualSerials.isEmpty()) {
                String sn = manualSerials.get(i);
                if (itemRepository.existsBySerialNumber(sn)) {
                    throw new RuntimeException("סריאל כפול! המספר " + sn + " כבר קיים.");
                }
                item.setSerialNumber(sn);
            } else if ("AUTO".equals(form.getSerialMode()) && form.getStartSerial() != null && !form.getStartSerial().isEmpty()) {
                try {
                    long start = Long.parseLong(form.getStartSerial());
                    item.setSerialNumber(String.valueOf(start + i));
                } catch (NumberFormatException e) {
                    // אם זה לא מספר, נשתמש בברירת מחדל של המערכת
                }
            }

            // --- העתקת הקיט מהתקן (Template) לפריט הספציפי ---
            // אנחנו משתמשים במידע שהגיע מה-Form (כדי לאפשר שינויים ידניים בטופס לפני השמירה)
            if (form.isKit() && form.getKitComponents() != null && !form.getKitComponents().isEmpty()) {
                for (var compDto : form.getKitComponents()) {
                    KitComponent comp = new KitComponent();
                    comp.setItem(item);
                    comp.setComponentName(compDto.getComponentName());
                    comp.setSubCatalogNumber(compDto.getSubCatalogNumber());
                    comp.setExpectedQuantity(compDto.getQuantity());
                    comp.setActualQuantity(compDto.getQuantity());
                    comp.setStatus("AVAILABLE");
                    item.getComponents().add(comp);
                }
            }
            // גיבוי: אם לא הגיעו רכיבים בטופס אבל ה-Type מוגדר כקיט, ניקח מה-Template
            else if (itemType.isKit() && itemType.getKitTemplateComponents() != null) {
                for (KitTemplateComponent template : itemType.getKitTemplateComponents()) {
                    KitComponent comp = new KitComponent();
                    comp.setItem(item);
                    comp.setComponentName(template.getComponentName());
                    comp.setSubCatalogNumber(template.getSubCatalogNumber());
                    comp.setExpectedQuantity(template.getQuantity());
                    comp.setActualQuantity(template.getQuantity());
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

        KitComponent targetComp = item.getComponents().stream()
                .filter(c -> c.getComponentName().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Component " + componentName + " not found in kit"));

        targetComp.setActualQuantity(actualQty);

        if (actualQty < targetComp.getExpectedQuantity()) {
            targetComp.setStatus("MISSING");
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