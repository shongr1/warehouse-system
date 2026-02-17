package com.warehouse.system.repository;

import com.warehouse.system.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByItemType_IdAndSerialNumber(Long itemTypeId, String serialNumber);
    List<Item> findByWarehouseId(Long warehouseId);
    List<Item> findBySignedBy_Id(Long userId);
}

