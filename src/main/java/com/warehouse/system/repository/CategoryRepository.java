package com.warehouse.system.repository;

import com.warehouse.system.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * מחזיר את כל הקטגוריות ששייכות למחסן ספציפי.
     * שימושי להצגת דף מחסן מחולק לקטגוריות.
     */
    List<Category> findByWarehouseId(Long warehouseId);
    Optional<Category> findByName(String name);
    /**
     * בודק אם קיימת קטגוריה בשם מסוים בתוך מחסן ספציפי.
     * עוזר למנוע כפילויות של שמות קטגוריות באותו מחסן.
     */
    boolean existsByNameAndWarehouseId(String name, Long warehouseId);
}