package com.warehouse.system.repository;

import com.warehouse.system.entity.KitComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KitComponentRepository extends JpaRepository<KitComponent, Long> {

    // אם השדה ב-KitComponent נקרא item:
    List<KitComponent> findByItem_Id(Long itemId);

    // או אם השדה נקרא kitType (כפי שהיה קודם):
    // List<KitComponent> findByKitType_Id(Long kitTypeId);
}