package com.warehouse.system.repository;

import com.warehouse.system.entity.KitComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitComponentRepository extends JpaRepository<KitComponent, Long> {

    List<KitComponent> findByKitType_Id(Long kitItemTypeId);

}