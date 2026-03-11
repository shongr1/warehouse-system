package com.warehouse.system.repository;

import com.warehouse.system.entity.Item;
import com.warehouse.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // --- שאילתת הליבה החדשה: סינון לפי המשתמש שהוסיף את המוצר ---
    List<Item> findByOwner(User owner);

    // במידה ואתה רוצה לשלוף לפי ID של משתמש ישירות
    List<Item> findByOwner_Id(Long userId);

    // --- בדיקות ייחודיות (Validation) ---
    boolean existsBySerialNumber(String serialNumber);
    boolean existsByItemType_IdAndSerialNumber(Long itemTypeId, String serialNumber);

    // --- שאילתת ה-Auto-Increment למק"ט רץ ---
    @Query(value = "SELECT MAX(CAST(SUBSTRING(internal_catalog_id, LENGTH(:prefix) + 1) AS INTEGER)) " +
            "FROM items WHERE internal_catalog_id LIKE :prefix", nativeQuery = true)
    Integer findMaxNumberByPrefix(@Param("prefix") String prefix);

    // --- שאילתות שליפה קיימות (שמרתי אותן עבורך) ---
    List<Item> findByCategoryId(Long categoryId);
    List<Item> findByWarehouseIdAndCategoryIsNull(Long warehouseId);
    List<Item> findByWarehouseId(Long warehouseId);
    List<Item> findBySignedBy_Id(Long userId);
    List<Item> findByWarehouseIdAndItemType_KitTrue(Long warehouseId);
}