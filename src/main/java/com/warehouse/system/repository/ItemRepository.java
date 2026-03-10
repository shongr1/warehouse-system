package com.warehouse.system.repository;

import com.warehouse.system.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // --- בדיקות ייחודיות (Validation) ---
    boolean existsBySerialNumber(String serialNumber);
    boolean existsByItemType_IdAndSerialNumber(Long itemTypeId, String serialNumber);

    // --- שאילתת ה-Auto-Increment למק"ט רץ ---
    // השאילתה מוציאה את המספר מהסוף של ה-internalCatalogId (למשל מתוך "S11-15" היא תוציא 15)
    @Query(value = "SELECT MAX(CAST(SUBSTRING(internal_catalog_id, LENGTH(:prefix) + 1) AS INTEGER)) " +
            "FROM items WHERE internal_catalog_id LIKE :prefix", nativeQuery = true)
    Integer findMaxNumberByPrefix(@Param("prefix") String prefix);

    // --- שאילתות שליפה (Queries) ---
    List<Item> findByCategoryId(Long categoryId);
    List<Item> findByWarehouseIdAndCategoryIsNull(Long warehouseId);
    List<Item> findByWarehouseId(Long warehouseId);
    List<Item> findBySignedBy_Id(Long userId);
    List<Item> findByWarehouseIdAndItemType_KitTrue(Long warehouseId);}