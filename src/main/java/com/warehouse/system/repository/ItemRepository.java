package com.warehouse.system.repository;

import com.warehouse.system.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // --- בדיקות ייחודיות (Validation) ---

    // בודק אם סריאל קיים בכלל במערכת (מומלץ להשתמש בזה למניעת כפילויות גלובלית)
    boolean existsBySerialNumber(String serialNumber);

    // הבדיקה הקודמת שלך (ייחודיות בתוך אותו סוג מוצר)
    boolean existsByItemType_IdAndSerialNumber(Long itemTypeId, String serialNumber);

    // --- שאילתות שליפה (Queries) ---
    List<Item> findByCategoryId(Long categoryId);

    // בונוס: מוצאת את כל הפריטים במחסן מסוים שעדיין אין להם קטגוריה
    List<Item> findByWarehouseIdAndCategoryIsNull(Long warehouseId);
    // מציאת כל הפריטים במחסן מסוים
    List<Item> findByWarehouseId(Long warehouseId);

    // מציאת כל הפריטים שחתומים אצל משתמש מסוים (המתודה שהחזרנו)
    List<Item> findBySignedBy_Id(Long userId);

    // מציאת כל הערכות (Kits) במחסן מסוים
    List<Item> findByWarehouseIdAndItemType_KitTrue(Long warehouseId);
}