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

        List<String> serials = parseSerials(form);

        // 1. בדיקת כפילויות סריאלים (לפני שמתחילים לשמור בכלל!)
        for (String sn : serials) {
            if (itemRepository.existsBySerialNumber(sn)) {
                throw new RuntimeException("סריאל כפול! המספר " + sn + " כבר קיים במערכת.");
            }
        }

        // 2. יצירת ה-Items ושמירתם
        for (String sn : serials) {
            Item item = new Item();
            item.setSerialNumber(sn);
            item.setItemType(itemType);
            item.setWarehouse(warehouse);
            item.setStatus(ItemStatus.IN_STOCK);

            // אם זו ערכה, כאן מוסיפים את ה-KitComponents (אופציונלי בהתאם ללוגיקה שלך)
            itemRepository.save(item);
        }

        // 3. עדכון ה-StockBalance (המונה הכללי)
        updateBalance(warehouse, itemType, serials.size());
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
        // לוגיקה שמפרידה סריאלים לפי שורות או פסיקים ומנקה רווחים
        if (form.getManualSerials() == null || form.getManualSerials().isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(form.getManualSerials().split("[,\n\r]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct() // מונע כפילויות בתוך הטופס עצמו
                .collect(Collectors.toList());
    }
}