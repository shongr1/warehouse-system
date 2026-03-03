package com.warehouse.system.repository;

import com.warehouse.system.entity.KitItemComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitItemComponentRepository extends JpaRepository<KitItemComponent, Long> {

    List<KitItemComponent> findByKitItem_Id(Long kitItemId);
    List<KitItemComponent> findByKitItem_IdOrderByKitComponent_Id(Long kitItemId);
    boolean existsByKitItem_Id(Long kitItemId);
}