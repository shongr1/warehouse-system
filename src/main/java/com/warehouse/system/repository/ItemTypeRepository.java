package com.warehouse.system.repository;

import com.warehouse.system.entity.ItemType;
import com.warehouse.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ItemTypeRepository extends JpaRepository<ItemType, Long> {

    // מוצא את המק"ט האחרון שמתחיל בקידומת מסוימת ושייך למשתמש ספציפי
    // אנחנו משתמשים ב-NATIVE QUERY כי קל יותר לטפל שם במחרוזות של SQL
    @Query(value = "SELECT catalog_number FROM item_types " +
            "WHERE owner_user_id = :ownerId " +
            "AND catalog_number LIKE :prefix% " +
            "ORDER BY catalog_number DESC LIMIT 1",
            nativeQuery = true)
    String findTopCatalogNumberByPrefix(@Param("prefix") String prefix, @Param("ownerId") Long ownerId);

    // פונקציה לבדיקה אם מק"ט כבר קיים (כדי למנוע כפילויות)
    boolean existsByCatalogNumber(String catalogNumber);
}